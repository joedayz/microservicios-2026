# 2. Event Sourcing: Diseño e Implementación

## Índice

1. [Paradigma de Event Sourcing](#paradigma-de-event-sourcing)
2. [Conceptos clave](#conceptos-clave)
3. [Diseño de eventos](#diseño-de-eventos)
4. [Proyecciones y CQRS](#proyecciones-y-cqrs)
5. [Snapshot](#snapshot)
6. [Implementación en Java](#implementación-en-java)
7. [Trade-offs](#trade-offs)

---

## Paradigma de Event Sourcing

### ¿Qué es Event Sourcing?

**Event Sourcing** es una patrón arquitectónico donde el estado de una entidad se **construye replaying (reproduciendo) una secuencia de eventos inmutables**, en lugar de almacenar el estado actual.

**Comparación**:

#### Modelo tradicional (CRUD)
```sql
-- Tabla: orders
UPDATE orders SET status = 'CONFIRMED' WHERE id = 'o1';

-- ¿Qué cambios tuvo antes? No hay historial.
-- ¿En qué momento falló? Desconocido.
```

#### Event Sourcing
```sql
-- Tabla: order_events (append-only)
INSERT INTO order_events (order_id, type, data, timestamp) 
VALUES ('o1', 'OrderCreated', {...}, '2026-01-01T10:00:00Z');

INSERT INTO order_events (order_id, type, data, timestamp) 
VALUES ('o1', 'StockReserved', {...}, '2026-01-01T10:01:00Z');

INSERT INTO order_events (order_id, type, data, timestamp) 
VALUES ('o1', 'OrderConfirmed', {...}, '2026-01-01T10:02:00Z');

-- Reproducir eventos en orden = reconstruir estado actual.
-- Tenemos historial completo y auditoría.
```

### Ventajas

| Ventaja | Descripción |
|---------|-------------|
| **Auditoría completa** | Cada cambio es un evento inmutable. Sabemos qué pasó y cuándo. |
| **Reproducibilidad** | Reproducir eventos desde el inicio = estado actual. |
| **Temporal query** | "¿Qué estado tenía la orden el 2026-01-01 a las 10:01?" |
| **Event replay para debugging** | Reproduce exactamente qué sucedió. |
| **Integración asíncrona** | Otros servicios se suscriben a eventos. |
| **Evolución sin migración** | Cambio de lógica se aplica solo a eventos futuros. |

### Desventajas

| Desventaja | Descripción |
|-----------|-------------|
| **Complejidad** | Más código, mental overhead. |
| **Eventual consistency** | Estado actual no es inmediato, requiere proyecciones. |
| **Storage** | Cada evento ocupa espacio. Necesita compaction (Kafka log compaction). |
| **Evolución de eventos** | Cambiar el esquema de eventos es delicado. |
| **Testing** | Más escenarios para cubrir. |

---

## Conceptos clave

### Eventos

Un **evento** es un cambio de estado **inmutable** que ya sucedió. Siempre en pasado.

```java
// ✅ Correcto: eventos en pasado
public class OrderCreatedEvent {
    String orderId;
    String customerId;
    LocalDateTime createdAt;
}

public class StockReservedEvent {
    String orderId;
    int quantity;
    LocalDateTime reservedAt;
}

// ❌ Incorrecto: "crear orden" es una acción, no un evento
public class CreateOrderCommand { }
```

#### Partes de un evento

```
┌─────────────────────────────────────────┐
│ Event                                   │
├─────────────────────────────────────────┤
│ eventId      : UUID                     │  Único identificador del evento
│ aggregateId  : String (orderId)         │  A qué entidad pertenece
│ eventType    : String (OrderCreated)    │  Tipo de evento
│ eventData    : JSON                     │  Payload del cambio
│ version      : Long (1, 2, 3...)        │  Versión dentro del aggregate
│ timestamp    : Instant                  │  Cuándo ocurrió
│ metadata     : Map (user, origin)       │  Contexto
└─────────────────────────────────────────┘
```

### Aggregate Root

Un **aggregate root** es la raíz de una entidad con estado mutable. Reúne:
- Identidad (`orderId`)
- Estado actual
- Colección de eventos no publicados (domain events)

```java
public class Order {
    private String orderId;
    private String customerId;
    private OrderStatus status;  // Estado actual
    private List<OrderEvent> uncommittedEvents;  // Eventos sin publicar
    
    // Métodos de negocio que generan eventos
    public void createOrder(String customerId) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId, customerId, Instant.now()
        );
        uncommittedEvents.add(event);
        apply(event);  // Aplicar al estado
    }
    
    public void reserveStock(int quantity) {
        StockReservedEvent event = new StockReservedEvent(
            orderId, quantity, Instant.now()
        );
        uncommittedEvents.add(event);
        apply(event);
    }
    
    // Reconstruir desde eventos
    public static Order fromHistory(List<OrderEvent> events) {
        Order order = new Order();
        for (OrderEvent event : events) {
            order.apply(event);
        }
        return order;
    }
    
    // Aplicar evento al estado
    private void apply(OrderEvent event) {
        if (event instanceof OrderCreatedEvent) {
            this.status = OrderStatus.CREATED;
        } else if (event instanceof StockReservedEvent) {
            this.status = OrderStatus.STOCK_RESERVED;
        }
    }
}
```

### Event Store

El **event store** es el almacenamiento de eventos (inmutable, append-only):

```java
public class EventStore {
    // Guardar eventos de un aggregate
    public void saveEvents(String aggregateId, List<Event> events) {
        for (Event event : events) {
            eventTable.insert(
                aggregateId,
                event.getEventType(),
                event.toJson(),
                event.getTimestamp()
            );
        }
    }
    
    // Recuperar historial de eventos
    public List<Event> getEvents(String aggregateId) {
        return eventTable.selectByAggregateId(aggregateId);
    }
}
```

### Versioning de eventos

Cuando el esquema de un evento cambia, necesitamos **versionamiento**:

```
// V1 (original)
class OrderCreatedEvent {
    String orderId;
    String customerId;
}

// V2 (agregamos campo)
class OrderCreatedEvent {
    String orderId;
    String customerId;
    String countryCode;  // NUEVO
}
```

**Estrategias**:

1. **Upcasting**: cuando deserializas V1, conviértelo a V2:
   ```java
   if (eventVersion == 1) {
       OrderCreatedEvent v2 = new OrderCreatedEvent(
           v1.orderId,
           v1.customerId,
           "US"  // default para eventos antigüos
       );
   }
   ```

2. **Schema evolution**: usar Avro con Schema Registry (versionar automáticamente).

---

## Diseño de eventos

### Naming convention

```java
// ✅ Nombres descriptivos
public class OrderCreatedEvent { }
public class StockReservedEvent { }
public class PaymentProcessedEvent { }
public class OrderShippedEvent { }

// ❌ Nombres genéricos
public class OrderChangedEvent { }
public class Event1 { }
```

### Contenido de un evento

Un evento debe contener **todo lo necesario** para reconstruir el cambio de estado:

```java
// ✅ Contiene datos suficientes
public class StockReservedEvent {
    String orderId;
    String productId;
    int quantity;
    LocalDateTime reservedAt;
    String warehouseId;  // Dónde se reservó
}

// ❌ Falta información
public class StockReservedEvent {
    String orderId;
    // ¿Cuánto? ¿Dónde? ¿Cuándo? Desconocido
}
```

### Immutabilidad

Los eventos deben ser **inmutables**:

```java
// ✅ Immutable
public record StockReservedEvent(
    String orderId,
    int quantity,
    LocalDateTime reservedAt
) {}

// ❌ Mutable
public class StockReservedEvent {
    public String orderId;  // public = mutable
    public int quantity;
    
    public void setQuantity(int qty) {  // setter = puede cambiar
        this.quantity = qty;
    }
}
```

### Información temporal

Incluir siempre **cuándo ocurrió**:

```java
public class OrderCreatedEvent {
    String orderId;
    String customerId;
    LocalDateTime createdAt;  // ← IMPORTANTE
    LocalDateTime processingTime;  // Para auditoría
}
```

---

## Proyecciones y CQRS

### Problema: Event Sourcing sin proyecciones

```java
// Recuperar estado actual de una orden
List<Event> events = eventStore.getEvents("order-123");
Order order = Order.fromHistory(events);  // ← Replay todos los eventos
```

**Problema**: Si una orden tiene 10,000 eventos, replays 10,000 eventos cada vez.

### Solución: Proyecciones

Una **proyección** es una vista desnormalizada **del estado actual**, actualizada por eventos:

```
Event Store (append-only)
    ↓ (Lee eventos)
Projection Handler (listener)
    ↓ (Actualiza)
Projection DB (tabla desnormalizada)
    ↓ (Consulta rápida)
Query Model (read-only)
```

#### Proyección simple

```java
// 1. Event Store: guardar eventos
eventStore.saveEvents("order-123", [OrderCreatedEvent, StockReservedEvent]);

// 2. Projection Listener: escuchar eventos
@Component
public class OrderProjection {
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // Actualizar tabla desnormalizada
        orderSummary.insert(
            event.orderId(),
            event.customerId(),
            OrderStatus.CREATED,
            Instant.now()
        );
    }
    
    @EventListener
    public void onStockReserved(StockReservedEvent event) {
        // Actualizar estado
        orderSummary.updateStatus(
            event.orderId(),
            OrderStatus.STOCK_RESERVED
        );
    }
}

// 3. Query: consulta rápida (no replay)
public Order getOrder(String orderId) {
    return orderSummary.findById(orderId);  // ← O(1) lookup
}
```

### CQRS: Command Query Responsibility Segregation

**CQRS** combina Event Sourcing con **modelos separados para lectura y escritura**:

```
Command Side (Write)              Query Side (Read)
    ↓                                 ↓
Order Service                    OrderProjection DB
(Event Sourcing)                 (Desnormalizado)
    ↓                                 
    └──→ Eventos ──→ [Event Bus] ──→ Actualizar proyección
```

**Ventajas**:
- ✅ Lectura: tabla optimizada, sin replay.
- ✅ Escritura: Event Sourcing, auditoría completa.
- ✅ Escalabilidad: read y write en BD diferentes.

**Desventajas**:
- ❌ Consistencia eventual: lectura puede estar atrás.
- ❌ Más complejidad.

---

## Snapshot

### Problema: muchos eventos = replay lento

Una orden con 5 años de historial puede tener 100,000 eventos. Replays cada evento = lento.

### Solución: Snapshots

Un **snapshot** es una "foto" del estado en un punto en el tiempo:

```
Events 1-100,000: replay 100,000
                → estado en momento N

Snapshot (en evento 50,000): {status: SHIPPED, total: 999.99}
Events 50,001-100,000: replay solo 50,000
                → estado en momento N (más rápido)
```

#### Implementación

```java
public class SnapshotStore {
    // Guardar snapshot cada N eventos
    public void saveSnapshot(String aggregateId, Snapshot snapshot) {
        snapshotTable.insert(
            aggregateId,
            snapshot.version(),  // A qué evento corresponde
            snapshot.state(),
            Instant.now()
        );
    }
    
    // Recuperar último snapshot + eventos posteriores
    public Order reconstructFromSnapshot(String aggregateId) {
        Snapshot snapshot = snapshotTable.getLatest(aggregateId);
        
        Order order = Order.fromSnapshot(snapshot);  // Cargar desde snapshot
        
        // Replay solo eventos posteriores al snapshot
        List<Event> recentEvents = eventStore.getEventsAfterVersion(
            aggregateId,
            snapshot.version()
        );
        for (Event event : recentEvents) {
            order.apply(event);
        }
        
        return order;
    }
}
```

**Configuración**:
```
Crear snapshot cada 100 eventos
Crear snapshot cada 1 hora
```

---

## Implementación en Java

### Paso 1: Definir eventos

```java
// Domain events
public abstract class DomainEvent {
    private String aggregateId;
    private long version;
    private Instant timestamp;
    
    public DomainEvent(String aggregateId, long version) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.timestamp = Instant.now();
    }
}

public class OrderCreatedEvent extends DomainEvent {
    private String customerId;
    private List<LineItem> items;
    
    public OrderCreatedEvent(String orderId, String customerId, List<LineItem> items) {
        super(orderId, 1);
        this.customerId = customerId;
        this.items = items;
    }
}

public class StockReservedEvent extends DomainEvent {
    private int reservedQuantity;
    
    public StockReservedEvent(String orderId, long version, int quantity) {
        super(orderId, version);
        this.reservedQuantity = quantity;
    }
}
```

### Paso 2: Implementar Aggregate Root

```java
public class Order {
    private String orderId;
    private String customerId;
    private OrderStatus status;
    private int reservedQuantity;
    private long version;
    private List<DomainEvent> uncommittedEvents;
    
    private Order() {
        this.uncommittedEvents = new ArrayList<>();
        this.version = 0;
    }
    
    // Comandos que generan eventos
    public static Order createOrder(String orderId, String customerId, List<LineItem> items) {
        Order order = new Order();
        order.orderId = orderId;
        
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerId, items);
        order.apply(event);
        order.uncommittedEvents.add(event);
        
        return order;
    }
    
    public void reserveStock(int quantity) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot reserve stock for status: " + status);
        }
        
        StockReservedEvent event = new StockReservedEvent(orderId, version + 1, quantity);
        apply(event);
        uncommittedEvents.add(event);
    }
    
    // Aplicar evento al estado
    private void apply(DomainEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.customerId = e.getCustomerId();
            this.status = OrderStatus.CREATED;
            this.version = e.getVersion();
        } else if (event instanceof StockReservedEvent e) {
            this.reservedQuantity = e.getReservedQuantity();
            this.status = OrderStatus.STOCK_RESERVED;
            this.version = e.getVersion();
        }
    }
    
    // Reconstruir desde historial
    public static Order fromHistory(String orderId, List<DomainEvent> events) {
        Order order = new Order();
        order.orderId = orderId;
        
        for (DomainEvent event : events) {
            order.apply(event);
        }
        
        return order;
    }
    
    public List<DomainEvent> getUncommittedEvents() {
        return uncommittedEvents;
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
```

### Paso 3: Event Store con base de datos

```java
@Component
public class EventStoreRepository {
    
    @Autowired
    private JdbcTemplate jdbc;
    
    public void saveEvents(String aggregateId, List<DomainEvent> events) {
        for (DomainEvent event : events) {
            jdbc.update(
                """
                INSERT INTO events (aggregate_id, event_type, event_data, version, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """,
                aggregateId,
                event.getClass().getSimpleName(),
                serializeEvent(event),  // JSON
                event.getVersion(),
                event.getTimestamp()
            );
        }
    }
    
    public List<DomainEvent> getEvents(String aggregateId) {
        return jdbc.query(
            "SELECT * FROM events WHERE aggregate_id = ? ORDER BY version ASC",
            (rs, idx) -> deserializeEvent(
                rs.getString("event_type"),
                rs.getString("event_data")
            ),
            aggregateId
        );
    }
    
    private String serializeEvent(DomainEvent event) {
        return new ObjectMapper().writeValueAsString(event);
    }
    
    private DomainEvent deserializeEvent(String type, String json) {
        ObjectMapper mapper = new ObjectMapper();
        return switch (type) {
            case "OrderCreatedEvent" -> mapper.readValue(json, OrderCreatedEvent.class);
            case "StockReservedEvent" -> mapper.readValue(json, StockReservedEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + type);
        };
    }
}
```

### Paso 4: Servicio que orquesta

```java
@Service
public class OrderService {
    
    @Autowired
    private EventStoreRepository eventStore;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public String createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        // 1. Crear order (genera OrderCreatedEvent)
        Order order = Order.createOrder(
            orderId,
            request.getCustomerId(),
            request.getItems()
        );
        
        // 2. Guardar eventos
        eventStore.saveEvents(orderId, order.getUncommittedEvents());
        
        // 3. Publicar eventos al bus de aplicación
        for (DomainEvent event : order.getUncommittedEvents()) {
            eventPublisher.publishEvent(event);
        }
        
        // 4. Marcar como committed
        order.markEventsAsCommitted();
        
        return orderId;
    }
    
    public Order getOrder(String orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId);
        return Order.fromHistory(orderId, events);
    }
}
```

---

## Trade-offs

| Aspecto | Beneficio | Costo |
|--------|----------|-------|
| **Auditoría** | Historial completo de cambios | Storage + indexación |
| **Debugging** | Replay exacto de eventos | Mental model + testing |
| **Escalabilidad** | Lectura y escritura separadas (CQRS) | Consistencia eventual |
| **Temporal queries** | "Estado en momento X" | Queries complejas |
| **Evolución** | Cambio de lógica sin migración | Versionamiento de eventos |

---

## Resumen

| Concepto | Significado |
|----------|------------|
| **Event Sourcing** | Guardar eventos inmutables, reproducir para estado. |
| **Aggregate Root** | Entidad con identidad + métodos generadores de eventos. |
| **Event Store** | Almacenamiento append-only de eventos. |
| **Projection** | Vista desnormalizada del estado actual. |
| **CQRS** | Modelos separados para lectura (proyecciones) y escritura (ES). |
| **Snapshot** | Foto del estado cada N eventos para acelerar replay. |

---

**Siguiente**: [03-saga-patterns.md](03-saga-patterns.md)
