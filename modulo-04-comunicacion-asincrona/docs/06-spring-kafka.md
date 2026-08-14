# 7. Spring Kafka: Producers, Consumers, Templates

## Índice

1. [Dependencias](#dependencias)
2. [KafkaTemplate (Producer)](#kafkatemplate-producer)
3. [@KafkaListener (Consumer)](#kafkalistener-consumer)
4. [Error Handling](#error-handling)
5. [Transacciones](#transacciones)
6. [Testing](#testing)

---

## Dependencias

**pom.xml**:
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Avro serialization -->
<dependency>
  <groupId>io.confluent</groupId>
  <artifactId>kafka-avro-serializer</artifactId>
  <version>7.5.0</version>
</dependency>

<!-- Testing -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

## KafkaTemplate (Producer)

### Configuración básica

**application.yml**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all  # Esperar ACK de todos los replicas
      retries: 3
      properties:
        linger.ms: 10  # Agrupar mensajes (latencia vs throughput)
        batch.size: 16384
```

### Uso básico

```java
@Service
public class OrderProducer {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void publishEvent(String orderId, String event) {
        // Opción 1: Fire and forget
        kafkaTemplate.send("orders-topic", orderId, event);
        
        // Opción 2: Con callback
        kafkaTemplate.send("orders-topic", orderId, event)
            .addCallback(
                result -> log.info("Sent: {}", result.getRecordMetadata()),
                ex -> log.error("Failed", ex)
            );
        
        // Opción 3: Con timeout
        try {
            SendResult<String, String> result = 
                kafkaTemplate.send("orders-topic", orderId, event)
                    .get(10, TimeUnit.SECONDS);
            
            log.info("Offset: {}", result.getRecordMetadata().offset());
        } catch (TimeoutException e) {
            log.error("Timeout", e);
        }
    }
}
```

### Serialización tipada

Crear un **ProducerConfig** para tipos específicos:

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, OrderCreatedEvent> 
           orderCreatedProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                       "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                       StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                       AvroSerializer.class);
        configProps.put("schema.registry.url", 
                       "http://schema-registry:8081");
        
        return new DefaultProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> 
           orderCreatedKafkaTemplate() {
        return new KafkaTemplate<>(orderCreatedProducerFactory());
    }
}

// Uso
@Service
public class OrderServiceAvro {
    
    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send("order-events", event.getOrderId(), event);
    }
}
```

### Partitioner personalizado

```java
public class OrderIdPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes,
                        Cluster cluster) {
        if (key == null) {
            return 0;
        }
        
        // Todas las órdenes del mismo cliente van a la misma partition
        String customerId = ((String) key).split("-")[0];
        return Math.abs(customerId.hashCode()) % cluster.partitionsForTopic(topic).size();
    }
}

// Configurar
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, OrderCreatedEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, 
                 OrderIdPartitioner.class);
        return new DefaultProducerFactory<>(props);
    }
}
```

---

## @KafkaListener (Consumer)

### Básico

```java
@Component
public class OrderConsumer {
    
    @KafkaListener(topics = "order-events", groupId = "order-processing")
    public void handleOrderCreated(String message) {
        log.info("Received: {}", message);
    }
}
```

### Con tipo específico

```java
@Component
public class OrderConsumer {
    
    @KafkaListener(topics = "order-events", groupId = "order-processing")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Order: {}, Customer: {}", 
                 event.getOrderId(), event.getCustomerId());
        
        processOrder(event);
    }
}
```

### Configuración avanzada

```java
@Component
public class OrderConsumer {
    
    @KafkaListener(
        topics = {"order-events", "order-cancelled"},  // Múltiples topics
        groupId = "order-processing",
        concurrency = "3"  // 3 threads en paralelo
    )
    public void handleOrderEvent(OrderEvent event, 
                                 @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Partition: {}, Offset: {}, Event: {}", 
                 partition, offset, event);
        
        processOrderEvent(event);
    }
}
```

### Commit manual

```java
@Component
public class OrderConsumer {
    
    @KafkaListener(topics = "order-events", groupId = "order-processing")
    public void handleOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
        try {
            OrderCreatedEvent event = record.value();
            processOrder(event);
            
            // Solo commitear después de éxito
            // (si auto-commit está deshabilitado)
        } catch (Exception e) {
            log.error("Processing failed, will retry", e);
            // No commitear → Kafka reentregará el mensaje
            throw e;
        }
    }
}

// Configuración
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> 
           orderCreatedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-processing");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // ← Manual
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultConsumerFactory<>(props);
    }
}
```

### Batch processing

```java
@Component
public class OrderConsumer {
    
    @KafkaListener(topics = "order-events", groupId = "order-processing")
    public void handleOrderBatch(List<OrderCreatedEvent> events) {
        log.info("Processing batch of {} orders", events.size());
        
        // Procesar todos juntos (más eficiente)
        processBatch(events);
    }
}

// Configuración
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>
           kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory 
            = new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setBatchListener(true);  // ← Enable batch
        factory.setConcurrency(3);
        
        return factory;
    }
}
```

---

## Error Handling

### Global error handler

```java
@Component
public class KafkaErrorHandler implements org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler {
    
    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception,
                             Consumer<?, ?> consumer) {
        log.error("Error processing message", exception);
        
        // Opciones:
        // 1. Log y continuar (skip mensaje)
        // 2. Publicar a DLT (Dead Letter Topic)
        // 3. Reintentar (con backoff)
        
        return null;
    }
}
```

### Dead Letter Topic (DLT)

```java
@Configuration
public class KafkaErrorHandling {
    
    @Bean
    public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        
        // Configurar DLT
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                new FixedBackOff(1000, 3)  // 3 reintentos, 1s delay
            )
        );
        
        return factory;
    }
    
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
           KafkaTemplate<String, String> template) {
        return new DeadLetterPublishingRecoverer(template,
            (record, ex) -> new TopicPartition("order-events-dlt", 0)
        );
    }
}

// DLT Listener
@Component
public class DLTHandler {
    
    @KafkaListener(topics = "order-events-dlt", groupId = "dlt-handler")
    public void handleDLT(OrderCreatedEvent event, 
                         @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("DLT - Event: {}, Error: {}", event, exceptionMessage);
        
        // Alertar a ops, guardar para revisión manual
    }
}
```

---

## Transacciones

### Exactly-once semantics (EOS)

```java
@Configuration
public class KafkaTransactionConfig {
    
    @Bean
    public ProducerFactory<String, OrderCreatedEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-service-1");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultProducerFactory<>(props);
    }
    
    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return new DefaultConsumerFactory<>(props);
    }
    
    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

// Uso
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    @Transactional
    public void processOrderWithTransaction(CreateOrderRequest request) {
        // 1. Guardar orden en BD
        Order order = orderRepository.save(new Order(...));
        
        // 2. Publicar evento (dentro de transacción)
        kafkaTemplate.send("order-events", order.getId(), 
            new OrderCreatedEvent(...));
        
        // Si cualquier cosa falla → rollback en BD y Kafka
    }
}
```

---

## Testing

### Test unitario con EmbeddedKafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9092",
    "port=9092"
})
class OrderServiceTest {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private CountDownLatch latch;
    private String receivedValue;
    
    @BeforeEach
    void setUp() {
        latch = new CountDownLatch(1);
    }
    
    @Test
    void testOrderPublishing() throws InterruptedException {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            "c1", "p1", 5
        );
        
        // When
        String orderId = orderService.createOrder(request);
        
        // Then
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        
        Order savedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals("PENDING", savedOrder.getStatus());
    }
    
    // Listener para capturar evento
    @KafkaListener(topics = "order-events", groupId = "test-group")
    public void listen(OrderCreatedEvent event) {
        receivedValue = event.getOrderId();
        latch.countDown();
    }
}
```

### Test de integración

```java
@SpringBootTest
class OrderIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CatalogService catalogService;
    
    @Autowired
    private KafkaTemplate<String, ?> kafkaTemplate;
    
    @Test
    void testOrderToInventoryFlow() throws InterruptedException {
        // 1. Crear orden
        String orderId = orderService.createOrder(...);
        
        // 2. Esperar a que Catalog procese
        Thread.sleep(2000);
        
        // 3. Verificar que Inventory se actualizó
        int stock = inventoryService.getStock("p1");
        assertEquals(95, stock);  // 100 - 5 reservados
    }
}
```

---

## Resumen

| Componente | Propósito |
|-----------|-----------|
| **KafkaTemplate** | Publicar eventos de forma síncrona o asíncrona. |
| **@KafkaListener** | Consumir eventos con callbacks automáticos. |
| **Partitioner** | Lógica personalizada para asignar a partitions. |
| **Error Handler** | Manejar fallos (DLT, reintentos, skip). |
| **Transactions** | Garantizar exactly-once (EOS). |

---

**Siguiente**: [8. Quarkus Messaging](07-quarkus-messaging.md)
