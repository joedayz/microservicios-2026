# 8. Quarkus Messaging: MicroProfile Reactive, SmallRye

## Índice

1. [Setup y dependencias](#setup-y-dependencias)
2. [Emitter (Producer)](#emitter-producer)
3. [@Incoming / @Outgoing (Consumer)](#incoming--outgoing-consumer)
4. [Channels y connectors](#channels-y-connectors)
5. [Error Handling](#error-handling)
6. [Testing](#testing)

---

## Setup y dependencias

**pom.xml**:
```xml
<!-- Quarkus Kafka -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-reactive-messaging-kafka</artifactId>
</dependency>

<!-- Avro -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-confluent-registry</artifactId>
</dependency>

<!-- Health checks -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

**application.properties**:
```properties
# Kafka broker
kafka.bootstrap.servers=localhost:9092

# Health checks
mp.messaging.outgoing.orders.connector=smallrye-kafka
mp.messaging.outgoing.orders.topic=order-events
mp.messaging.outgoing.orders.value.serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
mp.messaging.outgoing.orders.properties.schema.registry.url=http://schema-registry:8081

mp.messaging.incoming.orders-in.connector=smallrye-kafka
mp.messaging.incoming.orders-in.topic=order-events
mp.messaging.incoming.orders-in.group.id=inventory-group
mp.messaging.incoming.orders-in.value.deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer
mp.messaging.incoming.orders-in.properties.schema.registry.url=http://schema-registry:8081
```

---

## Emitter (Producer)

### Inyección de Emitter

```java
@ApplicationScoped
public class OrderProducer {
    
    @Inject
    @Channel("orders")  // ← Define el channel
    Emitter<OrderCreatedEvent> ordersEmitter;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        // Fire and forget
        ordersEmitter.send(event);
    }
    
    public CompletionStage<Void> publishOrderCreatedAsync(OrderCreatedEvent event) {
        // Con callback
        return ordersEmitter.send(event)
            .exceptionally(e -> {
                log.error("Failed to send", e);
                return null;
            });
    }
}
```

### Configuración de Emitter

En **application.properties**:
```properties
# Outgoing channel
mp.messaging.outgoing.orders.connector=smallrye-kafka
mp.messaging.outgoing.orders.topic=order-events
mp.messaging.outgoing.orders.value.serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
mp.messaging.outgoing.orders.properties.schema.registry.url=http://schema-registry:8081
mp.messaging.outgoing.orders.acks=all
mp.messaging.outgoing.orders.compression.type=snappy
```

### Envío con metadata

```java
public void publishWithMetadata(OrderCreatedEvent event) {
    Message<OrderCreatedEvent> message = Message.of(event)
        .withMetadata(new OutgoingKafkaRecordMetadata.Builder()
            .withKey(event.getOrderId())  // ← Partition key
            .withTimestamp(System.currentTimeMillis())
            .withHeaders(Map.of(
                "tracing-id", "trace-123",
                "source", "order-service"
            ))
            .build()
        );
    
    ordersEmitter.send(message);
}
```

---

## @Incoming / @Outgoing (Consumer)

### Listener simple

```java
@ApplicationScoped
public class OrderConsumer {
    
    @Incoming("orders-in")  // ← Channel de entrada
    public void consume(OrderCreatedEvent event) {
        log.info("Received order: {}", event.getOrderId());
        processOrder(event);
    }
}
```

### Con metadata

```java
@ApplicationScoped
public class OrderConsumer {
    
    @Incoming("orders-in")
    public void consume(Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        
        IncomingKafkaRecordMetadata metadata = 
            message.getMetadata(IncomingKafkaRecordMetadata.class)
                .orElse(null);
        
        if (metadata != null) {
            int partition = metadata.getPartition();
            long offset = metadata.getOffset();
            log.info("Partition: {}, Offset: {}", partition, offset);
        }
        
        processOrder(event);
        message.ack();  // Confirmar consumo
    }
}
```

### Procesamiento con transformación (@Outgoing)

```java
@ApplicationScoped
public class OrderProcessor {
    
    @Incoming("orders-in")
    @Outgoing("reserved-orders")  // Publicar resultado
    public ReservedOrderEvent processOrder(OrderCreatedEvent event) {
        // Procesar
        int reservedQuantity = reserveStock(event.getProductId());
        
        // Transformar y retornar
        return new ReservedOrderEvent(
            event.getOrderId(),
            event.getCustomerId(),
            reservedQuantity
        );
    }
}

// Listener para el channel de salida
@ApplicationScoped
public class ReservationListener {
    
    @Incoming("reserved-orders")
    public void consumeReserved(ReservedOrderEvent event) {
        log.info("Stock reserved: {} units", event.getQuantity());
    }
}
```

### Procesamiento múltiple (split)

```java
@ApplicationScoped
public class OrderProcessor {
    
    @Incoming("orders-in")
    @Outgoing("confirmed-orders")
    @Outgoing("failed-orders")
    public Multi<Message<? extends OrderEvent>> processMultiple(
            Message<OrderCreatedEvent> message) {
        OrderCreatedEvent event = message.getPayload();
        
        try {
            validateOrder(event);
            
            // Retornar evento de éxito
            return Multi.createFrom().item(
                Message.of(new OrderConfirmedEvent(event.getOrderId()))
                    .withMetadata(message.getMetadata())
            );
        } catch (Exception e) {
            // Retornar evento de fallo
            return Multi.createFrom().item(
                Message.of(new OrderFailedEvent(event.getOrderId(), e.getMessage()))
                    .withMetadata(message.getMetadata())
            );
        }
    }
}
```

---

## Channels y connectors

### Definir canales en application.properties

```properties
# Incoming (Consumer)
mp.messaging.incoming.orders-in.connector=smallrye-kafka
mp.messaging.incoming.orders-in.topic=order-events
mp.messaging.incoming.orders-in.group.id=inventory-group
mp.messaging.incoming.orders-in.auto.offset.reset=earliest
mp.messaging.incoming.orders-in.batch=false
mp.messaging.incoming.orders-in.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer

# Outgoing (Producer)
mp.messaging.outgoing.reserved-orders.connector=smallrye-kafka
mp.messaging.outgoing.reserved-orders.topic=stock-reserved
mp.messaging.outgoing.reserved-orders.value.serializer=org.apache.kafka.common.serialization.StringSerializer
```

### Batch processing

```properties
# Procesar en lotes
mp.messaging.incoming.orders-batch.connector=smallrye-kafka
mp.messaging.incoming.orders-batch.topic=order-events
mp.messaging.incoming.orders-batch.batch=true
mp.messaging.incoming.orders-batch.batch.size=100
```

```java
@ApplicationScoped
public class BatchOrderProcessor {
    
    @Incoming("orders-batch")
    public void processBatch(List<OrderCreatedEvent> events) {
        log.info("Processing batch of {} orders", events.size());
        
        // Procesar todos juntos
        for (OrderCreatedEvent event : events) {
            processOrder(event);
        }
    }
}
```

### Backpressure

```java
@ApplicationScoped
public class BackpressureHandler {
    
    @Incoming("orders-in")
    public CompletionStage<Void> consumeWithBackpressure(
            OrderCreatedEvent event) {
        // Retornar CompletionStage para controlar backpressure
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);  // Simular larga operación
                processOrder(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
```

---

## Error Handling

### Manejo básico con try-catch

```java
@ApplicationScoped
public class OrderConsumer {
    
    @Incoming("orders-in")
    public void consume(Message<OrderCreatedEvent> message) {
        try {
            OrderCreatedEvent event = message.getPayload();
            processOrder(event);
            message.ack();
        } catch (ProcessingException e) {
            log.error("Processing failed", e);
            message.nack(e);  // ← Nack para reintento
        }
    }
}
```

### Dead Letter Topic (DLT)

```properties
# Configurar DLT
mp.messaging.incoming.orders-in.failure-strategy=dead-letter-queue
mp.messaging.incoming.orders-in.dead-letter-queue.topic=order-events-dlt
```

```java
@ApplicationScoped
public class DLTHandler {
    
    @Incoming("order-events-dlt")
    public void handleDLT(Message<OrderCreatedEvent> message) {
        log.error("DLT - Failed message: {}", message.getPayload());
        
        // Alertar, guardar para revisión manual
    }
}
```

### Retry

```properties
# Reintentar
mp.messaging.incoming.orders-in.failure-strategy=retry
mp.messaging.incoming.orders-in.max-retries=3
mp.messaging.incoming.orders-in.initial-retry-delay.ms=1000
mp.messaging.incoming.orders-in.retry-backoff.type=exponential
mp.messaging.incoming.orders-in.retry-backoff.max-delay.ms=10000
```

---

## Health Checks

Quarkus integra health checks automáticamente:

```bash
curl http://localhost:8080/q/health
```

Respuesta:
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Kafka connection health check",
      "status": "UP",
      "data": {
        "kafka_broker": "localhost:9092"
      }
    }
  ]
}
```

Checks personalizados:

```java
@Liveness
@ApplicationScoped
public class KafkaHealthCheck implements HealthCheck {
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Verificar que Kafka está vivo
            KafkaConsumer<String, String> consumer = 
                new KafkaConsumer<>(Map.of(
                    "bootstrap.servers", "localhost:9092",
                    "group.id", "health-check"
                ));
            
            List<PartitionInfo> partitions = 
                consumer.listTopics().get("order-events", Duration.ofSeconds(2));
            
            consumer.close();
            
            return HealthCheckResponse.builder()
                .name("Kafka")
                .up()
                .withData("partitions", partitions.size())
                .build();
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                .name("Kafka")
                .down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}
```

---

## Testing

### Test unitario

```java
@QuarkusTest
class OrderConsumerTest {
    
    @Inject
    OrderConsumer consumer;
    
    @Inject
    @Any
    Instance<HealthCheckResponse> healthChecks;
    
    @Test
    void testConsumeOrder() {
        OrderCreatedEvent event = new OrderCreatedEvent(
            "o1", "c1", 100.0
        );
        
        // Simular mensaje
        Message<OrderCreatedEvent> message = Message.of(event);
        consumer.consume(message);
        
        // Verificar que se procesó
        assertTrue(orderRepository.findById("o1").isPresent());
    }
}
```

### Test con InMemoryConnector

```java
@QuarkusTest
class OrderProcessorTest {
    
    @Inject
    OrderProcessor processor;
    
    @Inject
    @Any
    InMemoryConnector connector;
    
    @BeforeEach
    void setup() {
        connector.reset();
    }
    
    @Test
    void testOrderProcessing() {
        // Enviar evento
        OrderCreatedEvent event = new OrderCreatedEvent("o1", "c1", 100.0);
        connector.source("orders-in").send(event);
        
        // Verificar resultado en canal de salida
        List<? extends Message<?>> messages = 
            connector.sink("confirmed-orders").received();
        
        assertEquals(1, messages.size());
        
        Message<?> result = messages.get(0);
        assertTrue(result.getPayload() instanceof OrderConfirmedEvent);
    }
}
```

---

## Comparación: Spring Kafka vs Quarkus Messaging

| Aspecto | Spring Kafka | Quarkus Messaging |
|--------|-------------|------------------|
| **Paradigma** | Imperativo (Template) | Reactivo (Streams) |
| **Sintaxis** | Annotations + beans | Annotations simples |
| **Performance** | Bueno | Excelente (native image) |
| **GraalVM Native** | Requiere config | Soporte nativo |
| **Learning curve** | Medio | Medio-Alto |
| **Comunidad** | Muy grande | Creciente |

**Regla de oro**:
- **Spring Boot**: Equipos grandes, ecosistema Spring.
- **Quarkus**: Containers, serverless, native image.

---

## Resumen

| Componente | Propósito |
|-----------|-----------|
| **Emitter<T>** | Publicar eventos (producer). |
| **@Incoming** | Consumir eventos de un channel. |
| **@Outgoing** | Publicar a un channel (resultado de procesamiento). |
| **Channel** | Configuración de topic + serialization. |
| **Message<T>** | Wrapper con payload + metadata. |

---

**Siguiente**: [9. Operacionalización](09-operacionalizacion.md)
