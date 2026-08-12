# 4. Transactional Outbox y Change Data Capture (CDC)

## Índice

1. [El problema: Dual Write](#el-problema-dual-write)
2. [Solución: Transactional Outbox](#solución-transactional-outbox)
3. [Change Data Capture (CDC)](#change-data-capture)
4. [Implementación en Java](#implementación-en-java)
5. [Herramientas: Debezium](#herramientas-debezium)

---

## El problema: Dual Write

### ¿Qué es Dual Write?

**Dual write** es cuando cambias BD y publicas a Kafka en dos operaciones separadas:

```java
// ❌ PROBLEMA: Dual Write
public void createOrder(Order order) {
    // Paso 1: Escribir en BD
    orderDb.insert(order);
    
    // Paso 2: Publicar a Kafka
    kafkaProducer.send("orders-topic", order);
    // ← Si se cae acá, orden está en BD pero no se publicó
}
```

### Escenarios de fallo

```
Escenario 1: Se cae después de Insert, antes de Kafka
    ├─ BD: Orden guardada ✓
    ├─ Kafka: Nada ✗
    └─ Resultado: Orden "perdida" (subscribers no saben)

Escenario 2: Se cae después de Kafka, antes de confirmar a cliente
    ├─ BD: Orden puede no estar guardada o en rollback
    ├─ Kafka: Evento publicado ✓
    └─ Resultado: Duplicado (subscribers procesan sin orden en BD)

Escenario 3: Red se parte (partition)
    ├─ BD: Cambio guardado
    ├─ Kafka: Inaccesible
    └─ Resultado: Inconsistencia
```

### ¿Por qué no solo usar Kafka?

```java
// ❌ También falla
kafkaProducer.send("orders-topic", order);  // OK
orderDb.insert(order);                       // FALLA
// Kafka tiene el evento pero BD no tiene la orden
```

### Garantías imposibles con Dual Write

- ❌ Atomicidad entre BD y Kafka.
- ❌ Garantía "exactamente una vez" confiable.
- ❌ Consistencia transaccional.

---

## Solución: Transactional Outbox

### Concepto

**Transactional Outbox** soluciona dual write usando una tabla **outbox** dentro de la MISMA BD:

```
Una transacción ACID local:
    ├─ INSERT INTO orders (...)
    └─ INSERT INTO outbox (aggregate_id, event_data, published=false)
    
Luego, separadamente:
    Polling sobre outbox
    → SELECT * FROM outbox WHERE published = false
    → Publicar a Kafka
    → UPDATE outbox SET published = true
```

**Garantía**: Si el INSERT en orders falla, el INSERT en outbox también falla (misma transacción). Si falla entre publicar y marcar, el poller reintenta.

### Ventajas

✅ **Atomicidad local**: Cambio de datos + outbox = una transacción ACID.  
✅ **No requiere transacciones distribuidas** (2PC).  
✅ **Recuperable**: Si Kafka cae, outbox persiste.  
✅ **Confiable**: Pollers reintentan indefinidamente.

### Desventajas

❌ **Latencia**: Delay entre INSERT en outbox y publicación a Kafka (segundos).  
❌ **Complejidad**: Necesita tabla outbox + polling logic.  
❌ **Mantenimiento**: Limpiar registros publicados periodicamente.

### Diagrama

```
┌─────────────────────────────────────────┐
│ Order Service                           │
├─────────────────────────────────────────┤
│                                         │
│  1. Transacción ACID (una sola):        │
│     INSERT orders (order_id, ...)       │
│     INSERT outbox (order_id, event)     │
│                                         │
│  2. Polling (separado):                 │
│     SELECT * FROM outbox WHERE          │
│        published = false                │
│                                         │
│  3. Publicar a Kafka (async):           │
│     kafkaProducer.send(event)           │
│                                         │
│  4. Marcar como publicado:              │
│     UPDATE outbox SET published = true  │
│                                         │
└─────────────────────────────────────────┘
            ↓
        Kafka Broker
            ↓
    Subscribers (Catalog, Inventory, etc.)
```

---

## Change Data Capture (CDC)

### ¿Qué es CDC?

**CDC (Change Data Capture)** es un proceso que captura cambios en BD y los publica como eventos.

En lugar de **polling** (SELECT cada segundo), CDC **escucha el transaction log** de la BD.

### Tipos de CDC

#### 1. Query-based polling (simple)

```java
@Scheduled(fixedDelay = 1000)  // Cada segundo
public void pollOutbox() {
    List<OutboxRecord> unpublished = 
        jdbcTemplate.query("SELECT * FROM outbox WHERE published = false");
    
    for (OutboxRecord record : unpublished) {
        kafkaTemplate.send("orders-topic", record.getEventData());
        
        jdbcTemplate.update(
            "UPDATE outbox SET published = true WHERE id = ?",
            record.getId()
        );
    }
}
```

**Pros**: Simple, sin dependencias.  
**Contras**: Latencia (delay de 1 segundo), carga en BD.

#### 2. Trigger-based (más rápido)

Usar triggers de BD para detectar cambios:

```sql
CREATE TRIGGER outbox_trigger AFTER INSERT ON outbox
FOR EACH ROW
BEGIN
    -- Notificar a proceso listener
    NOTIFY outbox_changed;
END;
```

Proceso escucha `NOTIFY` (Postgres):

```java
@Component
public class OutboxListener {
    public void onOutboxChanged() {
        // Publicar a Kafka inmediatamente
    }
}
```

**Pros**: Casi instantáneo, menor carga.  
**Contras**: Specifico de BD.

#### 3. Log-based CDC (Debezium)

Lee el **transaction log** de la BD (WAL en Postgres, binlog en MySQL):

```
Database Transaction Log
    ├─ Evento: INSERT orders (...)
    ├─ Evento: INSERT outbox (...)
    └─ Evento: UPDATE outbox SET published = true

Debezium CDC Connector
    ↓ (Lee log continuamente)
    
Kafka Connect
    ↓ (Publica cambios)
    
orders-topic (Kafka)
    ├─ OrderCreated
    └─ OrderPublished
```

**Pros**: No requiere tabla outbox explícita, automático, de log.  
**Contras**: Depende de Kafka Connect, más infrastructure.

---

## Implementación en Java

### Paso 1: Crear tabla Outbox

```sql
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,  -- "Order", "Payment", etc.
    event_type VARCHAR(255) NOT NULL,       -- "OrderCreated", "OrderConfirmed", etc.
    payload JSON NOT NULL,
    published BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL
);

CREATE INDEX idx_unpublished ON outbox(published, created_at);
```

### Paso 2: Entity con Outbox

```java
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    private String id;
    
    private String customerId;
    
    private OrderStatus status;
    
    @Transient
    private List<OutboxEvent> outboxEvents = new ArrayList<>();
    
    // Métodos de negocio que generan eventos
    public void createOrder(String customerId) {
        this.status = OrderStatus.CREATED;
        
        // Agregar a outbox (no publicar aún)
        outboxEvents.add(new OutboxEvent(
            this.id,
            "Order",
            "OrderCreated",
            Map.of(
                "orderId", this.id,
                "customerId", customerId,
                "timestamp", Instant.now()
            )
        ));
    }
    
    public List<OutboxEvent> getOutboxEvents() {
        return outboxEvents;
    }
    
    public void clearOutboxEvents() {
        outboxEvents.clear();
    }
}

@Entity
@Table(name = "outbox")
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> payload;
    
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    
    public OutboxEvent(String aggregateId, String aggregateType, 
                       String eventType, Map<String, Object> payload) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.published = false;
        this.createdAt = LocalDateTime.now();
    }
}
```

### Paso 3: Repositorio y Transacción

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {}

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedFalse();
}

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Transactional  // ← UNA transacción ACID
    public String createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        // 1. Crear orden
        Order order = new Order(orderId, request.getCustomerId());
        order.createOrder(request.getCustomerId());  // Genera evento
        
        // 2. Guardar orden + outbox en MISMA transacción
        orderRepository.save(order);
        
        // 3. Guardar eventos en outbox
        for (OutboxEvent event : order.getOutboxEvents()) {
            outboxRepository.save(event);
        }
        
        order.clearOutboxEvents();
        
        return orderId;
    }
}
```

### Paso 4: Poller (Outbox Publisher)

```java
@Component
public class OutboxPoller {
    
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Scheduled(fixedDelay = 1000)  // Cada 1 segundo
    public void pollOutbox() {
        List<OutboxEvent> unpublished = outboxRepository.findByPublishedFalse();
        
        for (OutboxEvent event : unpublished) {
            try {
                // Publicar a Kafka
                String topic = event.getAggregateType().toLowerCase() + "-events";
                kafkaTemplate.send(topic, 
                    event.getAggregateId(),
                    objectMapper.writeValueAsString(event.getPayload())
                );
                
                // Marcar como publicado
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);
                
            } catch (Exception e) {
                logger.error("Failed to publish outbox event: " + event.getId(), e);
                // Reintentará en siguiente ciclo
            }
        }
    }
    
    // Limpiar registros publicados hace > 24h
    @Scheduled(fixedDelay = 86400000)  // Cada 24 horas
    public void cleanupPublishedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        outboxRepository.deleteByPublishedTrueAndPublishedAtBefore(cutoff);
    }
}
```

### Paso 5: Listener (Consumer)

```java
@Service
public class OrderEventListener {
    
    @KafkaListener(topics = "order-events", groupId = "catalog-group")
    public void handleOrderCreated(String message) throws IOException {
        Map<String, Object> payload = objectMapper.readValue(message, Map.class);
        String eventType = (String) payload.get("eventType");
        
        if ("OrderCreated".equals(eventType)) {
            String orderId = (String) payload.get("orderId");
            
            // Catalog Service valida stock
            validateStockAndReserve(orderId);
        }
    }
}
```

---

## Herramientas: Debezium

**Debezium** es un plataforma CDC que lee el transaction log de BD y publica a Kafka.

### Setup con Docker Compose

```yaml
version: '3'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: orders_db
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181

  kafka-connect:
    image: confluentinc/cp-kafka-connect:7.5.0
    ports:
      - "8083:8083"
    environment:
      CONNECT_BOOTSTRAP_SERVERS: kafka:9092
      CONNECT_REST_PORT: 8083
      CONNECT_GROUP_ID: connect
      CONNECT_CONFIG_STORAGE_TOPIC: connect-config
      CONNECT_OFFSET_STORAGE_TOPIC: connect-offsets
      CONNECT_STATUS_STORAGE_TOPIC: connect-status
```

### Crear Debezium Connector

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-outbox-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": 5432,
      "database.user": "postgres",
      "database.password": "password",
      "database.dbname": "orders_db",
      "table.include.list": "public.outbox",
      "plugin.name": "pgoutput",
      "publication.name": "dbz_publication",
      "slot.name": "dbz_slot",
      "topic.prefix": "orders"
    }
  }'
```

Debezium ahora:
1. Lee `outbox` table en Postgres.
2. Publica cambios a `orders.public.outbox` topic en Kafka.
3. Si BD cae, retoma desde donde dejó (exactamente una vez).

---

## Resumen

| Patrón | Latencia | Complejidad | Confiabilidad |
|--------|----------|-------------|---------------|
| **Dual Write** | Baja | Simple | Baja ❌ |
| **Outbox Polling** | Media (1-5s) | Media | Alta ✅ |
| **Outbox Trigger** | Baja (< 100ms) | Media | Alta ✅ |
| **Debezium CDC** | Baja (< 100ms) | Alta | Muy alta ✅ |

**Recomendación**:
- **MVP**: Outbox con polling (simple, confiable).
- **Producción**: Debezium (automático, robusto).

---

**Siguiente**: [05-schema-registry-avro.md](05-schema-registry-avro.md)
