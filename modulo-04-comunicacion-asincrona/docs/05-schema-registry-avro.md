# 5. Schema Registry y Avro: Evolución de Esquemas

## Índice

1. [El problema: JSON sin validación](#el-problema-json-sin-validación)
2. [Apache Avro: serializador tipado](#apache-avro)
3. [Schema Registry: versionamiento central](#schema-registry)
4. [Evolución sin breaking changes](#evolución-sin-breaking-changes)
5. [Implementación en Java](#implementación-en-java)

---

## El problema: JSON sin validación

### Enviar JSON plano

```java
// Producer
OrderCreatedEvent event = new OrderCreatedEvent(...);
String json = objectMapper.writeValueAsString(event);
kafkaProducer.send("orders-events", json);

// Consumer
String json = kafkaConsumer.receive();
OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);
// ¿Qué pasa si la estructura cambió?
```

### Problemas

1. **Sin validación**: Cualquier JSON pasa. ¿Requiere `customerId`? No hay forma de validar.

2. **Evolución riesgosa**: 
   ```java
   // v1: {orderId, customerId, amount}
   // v2: {orderId, customerId, amount, countryCode}  ← Campo nuevo
   
   // Consumer antiguo (v1):
   OrderCreatedEvent e = mapper.readValue(json);  // ← Ignora countryCode
   // Consumer nuevo podría esperar countryCode
   ```

3. **Sin versionamiento central**: Cada equipo decide qué versión soporta.

4. **Breaking changes**:
   ```java
   // v1: {orderId: string, quantity: int}
   // v2: {orderId: string, quantity: double}  // ← Cambio de tipo
   
   // Consumer antiguo no puede parsear v2
   ```

---

## Apache Avro

### ¿Qué es Avro?

**Apache Avro** es un formato de serialización tipado + comprimido:

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.example.order.events",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "timestamp", "type": "long"}  // milliseconds
  ]
}
```

### Ventajas sobre JSON

| Aspecto | JSON | Avro |
|--------|------|------|
| **Tipado** | Ninguno | Tipado (int, string, array, etc.) |
| **Tamaño** | ~200 bytes | ~50 bytes (4x comprimido) |
| **Validación** | Manual | Automática por schema |
| **Evolución** | Manual | Compatible automáticamente |
| **Velocidad** | Lento (parse JSON) | Rápido (binary) |

### Tipos Avro

```json
{
  "type": "record",
  "name": "Order",
  "fields": [
    {"name": "id", "type": "string"},
    
    // Primitivos
    {"name": "quantity", "type": "int"},
    {"name": "price", "type": "double"},
    {"name": "active", "type": "boolean"},
    
    // Nullables (union con null)
    {"name": "notes", "type": ["null", "string"], "default": null},
    
    // Array
    {"name": "items", "type": {"type": "array", "items": "string"}},
    
    // Enum
    {"name": "status", "type": {
      "type": "enum",
      "name": "OrderStatus",
      "symbols": ["PENDING", "CONFIRMED", "SHIPPED"]
    }},
    
    // Objeto anidado
    {"name": "shipping", "type": {
      "type": "record",
      "name": "Address",
      "fields": [
        {"name": "street", "type": "string"},
        {"name": "city", "type": "string"}
      ]
    }},
    
    // Timestamp
    {"name": "createdAt", "type": {
      "type": "long",
      "logicalType": "timestamp-millis"
    }}
  ]
}
```

---

## Schema Registry

### ¿Qué es Schema Registry?

**Schema Registry** es un repositorio centralizado que **versionea y gestiona esquemas Avro**:

```
┌─────────────────────────────────────────┐
│ Schema Registry (Central)                │
├─────────────────────────────────────────┤
│                                         │
│ Topic: order-events                     │
│   ├─ v1 (SchemaId: 1)                   │
│   │  {orderId, customerId, amount}      │
│   │                                     │
│   ├─ v2 (SchemaId: 2)                   │
│   │  {orderId, customerId, amount,      │
│   │   countryCode}  ← Compatible        │
│   │                                     │
│   └─ v3 (SchemaId: 3)                   │
│      {orderId, customerId, amount,      │
│       countryCode, currency}            │
│                                         │
└─────────────────────────────────────────┘
```

### Flujo

1. **Producer registra schema**:
   ```java
   SchemaRegistryClient client = new CachedSchemaRegistryClient(...);
   int schemaId = client.register("order-events", schema);  // v1 → ID 1
   ```

2. **Producer serializa**:
   ```java
   AvroSerializer serializer = new AvroSerializer(client);
   byte[] data = serializer.serialize("order-events", event);  // ID 1 prepended
   ```

3. **Kafka almacena**: `[schema_id: 1, data: {...}]`

4. **Consumer deserializa**:
   ```java
   AvroDeserializer deserializer = new AvroDeserializer(client);
   OrderCreatedEvent event = deserializer.deserialize("order-events", bytes);
   // Cliente automáticamente busca schema por ID
   ```

---

## Evolución sin breaking changes

### Reglas Avro

Avro define tres modos de compatibilidad:

#### 1. Backward Compatibility (default)

Nuevos consumers pueden leer eventos antiguos.

```json
// v1 (viejo)
{"orderId": "o1", "customerId": "c1", "amount": 99.99}

// v2 (nuevo) - BACKWARD COMPATIBLE
// Agregamos campo con default
{"orderId": "o1", "customerId": "c1", "amount": 99.99, "countryCode": "US"}

// Consumer v2 lee v1:
// ✅ orderId, customerId, amount ok
// ✅ countryCode = default "US"
```

**Schema v1** → **v2**:
```json
// v1
[
  {"name": "orderId", "type": "string"},
  {"name": "customerId", "type": "string"},
  {"name": "amount", "type": "double"}
]

// v2 (agregar campo con default)
[
  {"name": "orderId", "type": "string"},
  {"name": "customerId", "type": "string"},
  {"name": "amount", "type": "double"},
  {"name": "countryCode", "type": "string", "default": "US"}  // ← default requerido
]
```

**Reglas**:
- ✅ Agregar campo con `default`.
- ✅ Remover campo SIN default.
- ❌ Cambiar tipo de campo.
- ❌ Agregar campo SIN default.

#### 2. Forward Compatibility

Viejos consumers pueden leer eventos nuevos.

```json
// v1 (viejo consumer)
{"orderId": "o1", "customerId": "c1", "amount": 99.99}

// v2 (nuevo producer)
{"orderId": "o1", "customerId": "c1", "amount": 99.99, "countryCode": "US"}

// Consumer v1 lee v2:
// ✅ Ignora countryCode (unknown field)
```

**Reglas**:
- ✅ Agregar campo (old consumer lo ignora).
- ✅ Remover campo con `default`.
- ❌ Remover campo SIN default.

#### 3. Full Compatibility

Ambas direcciones (backward + forward).

Requiere ambas reglas. Más restrictivo.

### Configurar en Schema Registry

```bash
curl -X PUT http://schema-registry:8081/config/order-events \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "compatibilityLevel": "BACKWARD"  # o FORWARD o FULL
  }'
```

---

## Implementación en Java

### Paso 1: Definir esquemas Avro

```
src/main/resources/avro/
├── order-events.avsc
├── inventory-events.avsc
└── common.avsc
```

**order-events.avsc**:
```json
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "namespace": "com.example.order.events",
  "doc": "Event published when order is created",
  "fields": [
    {"name": "eventId", "type": "string", "doc": "Unique event ID"},
    {"name": "orderId", "type": "string", "doc": "Order ID"},
    {"name": "customerId", "type": "string", "doc": "Customer ID"},
    {"name": "amount", "type": "double", "doc": "Order amount"},
    {"name": "createdAt", "type": {
      "type": "long",
      "logicalType": "timestamp-millis"
    }, "doc": "Creation timestamp"}
  ]
}
```

### Paso 2: Generar clases desde schemas

**pom.xml**:
```xml
<plugin>
  <groupId>org.apache.avro</groupId>
  <artifactId>avro-maven-plugin</artifactId>
  <version>1.11.1</version>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>schema</goal>
      </goals>
      <configuration>
        <sourceDirectory>${project.basedir}/src/main/resources/avro</sourceDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Genera:
```java
public class OrderCreatedEvent extends SpecificRecordBase {
    public String getEventId() { ... }
    public String getOrderId() { ... }
    public String getCustomerId() { ... }
    public double getAmount() { ... }
    // ...
}
```

### Paso 3: Configurar Kafka con Avro

**pom.xml**:
```xml
<dependency>
  <groupId>io.confluent</groupId>
  <artifactId>kafka-avro-serializer</artifactId>
  <version>7.5.0</version>
</dependency>
```

**application.yml**:
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      acks: all
      properties:
        schema.registry.url: http://schema-registry:8081
    consumer:
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://schema-registry:8081
        specific.avro.reader: true
```

### Paso 4: Enviar eventos Avro

```java
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    public String createOrder(CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        // Crear evento Avro
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setOrderId(orderId)
            .setCustomerId(request.getCustomerId())
            .setAmount(request.getAmount())
            .setCreatedAt(System.currentTimeMillis())
            .build();
        
        // Enviar a Kafka (serializado con Avro)
        kafkaTemplate.send("order-events", orderId, event);
        
        return orderId;
    }
}
```

### Paso 5: Consumir eventos Avro

```java
@Service
public class CatalogService {
    
    @KafkaListener(topics = "order-events", groupId = "catalog-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order: {} from customer: {}", 
                 event.getOrderId(), event.getCustomerId());
        
        // Procesar evento
        validateStockAndReserve(event.getOrderId(), event.getAmount());
    }
}
```

### Paso 6: Registrar schema en Schema Registry

```bash
# Registrar schema v1
curl -X POST http://schema-registry:8081/subjects/order-events-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @src/main/resources/avro/order-events.avsc

# Respuesta: {"id": 1}
```

---

## Ejemplo: Evolución de esquema

### V1 original
```json
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "amount", "type": "double"}
  ]
}
```

Producer v1, Consumer v1: ✅ Compatible.

### V2: Agregar país

```json
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "countryCode", "type": "string", "default": "US"}  // ← NEW
  ]
}
```

- Producer v2 → Consumer v1: ✅ v1 ignora countryCode.
- Producer v1 → Consumer v2: ✅ v2 usa default "US".

### V3: Agregar moneda

```json
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "countryCode", "type": "string", "default": "US"},
    {"name": "currency", "type": "string", "default": "USD"}  // ← NEW
  ]
}
```

Todos los consumers (v1, v2, v3) pueden leer cualquier versión.

---

## Resumen

| Concepto | Propósito |
|----------|-----------|
| **Avro** | Serialización tipada + comprimida. |
| **Schema Registry** | Repositorio centralizado de esquemas con versionamiento. |
| **Backward Compatibility** | Nuevos consumers leen eventos antiguos. |
| **Forward Compatibility** | Viejos consumers leen eventos nuevos. |
| **SchemaId** | ID único para cada versión de esquema (prepended al payload). |

---

**Siguiente**: [6. Eventual Consistency](06-eventual-consistency.md)
