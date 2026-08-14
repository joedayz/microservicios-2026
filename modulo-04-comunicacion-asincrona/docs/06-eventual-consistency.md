# 6. Eventual Consistency y Coherencia en Microservicios

## 1. Concepto de Eventual Consistency

### Definición
Eventual Consistency es un modelo de consistencia débil donde los datos en un sistema distribuido convergen a un estado consistente después de un período de tiempo, sin garantizar consistencia inmediata.

```
Escritura → Propagación → Replicación → Consistencia
   (Inmediato)  (Red)      (Procesamiento)  (Eventual)
```

### CAP Theorem
```
    Consistency
        /\
       /  \
      /    \
     C      A
    /        \
   /          \
  P-----------P
Partition     Availability

En sistemas distribuidos, solo puedes garantizar 2 de 3:
- C: Consistencia (todos ven el mismo dato)
- A: Disponibilidad (sistema siempre responde)
- P: Tolerancia a Particiones (red puede fallar)

Microservicios eligen: A + P (sacrifican C)
→ Eventual Consistency
```

## 2. Patrones de Eventual Consistency

### 2.1 Read-Your-Own-Write (RYOW)
```java
@Service
public class OrderService {
    private final OrderRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderCache cache;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderCommand cmd) {
        // 1. Persiste en BD
        Order order = repository.save(cmd.toOrder());
        
        // 2. Actualiza cache local INMEDIATAMENTE
        cache.put(order.getId(), order);
        
        // 3. Publica evento (propagará a otros servicios)
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
        
        // 4. Retorna desde cache (el usuario VE su escritura)
        return OrderResponse.from(cache.get(order.getId()));
    }
    
    public OrderResponse getOrder(String orderId, String userId) {
        // Para el usuario que creó la orden: lee del cache primero
        if (isCurrentUser(userId)) {
            Order cached = cache.getIfPresent(orderId);
            if (cached != null) return OrderResponse.from(cached);
        }
        
        // Para otros: lee del repositorio (puede estar desactualizado)
        return OrderResponse.from(repository.findById(orderId));
    }
}
```

### 2.2 Conflict Resolution
```java
@Service
public class InventoryConflictResolver {
    
    // Estrategia 1: Last-Write-Wins
    public InventoryItem lastWriteWins(InventoryItem local, 
                                       InventoryItem remote) {
        if (remote.getVersion() > local.getVersion() ||
            (remote.getVersion() == local.getVersion() && 
             remote.getUpdatedAt().isAfter(local.getUpdatedAt()))) {
            return remote;
        }
        return local;
    }
    
    // Estrategia 2: Custom Business Logic
    public InventoryItem businessLogicResolution(InventoryItem local,
                                                 InventoryItem remote) {
        // Aplicar lógica de negocio
        if (local.getReservedCount() > remote.getReservedCount()) {
            // Usar la más restrictiva (menos stock disponible)
            InventoryItem merged = local.clone();
            merged.setReservedCount(
                Math.max(local.getReservedCount(), 
                        remote.getReservedCount())
            );
            return merged;
        }
        return remote;
    }
    
    // Estrategia 3: Operational Transformation
    public InventoryItem operationalTransform(InventoryItem base,
                                              InventoryItem operation1,
                                              InventoryItem operation2) {
        // CRDT-style merge
        if (operation1.getStockLevel() > operation2.getStockLevel()) {
            return operation1;
        }
        return operation2;
    }
}
```

### 2.3 Convergence Guarantees
```java
@Service
public class ConsistencyChecker {
    private final OrderRepository orderRepo;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    // Verificar convergencia periódicamente
    @Scheduled(fixedRate = 60000)
    public void checkConsistency() {
        List<Order> orders = orderRepo.findAll();
        
        for (Order order : orders) {
            // Verificar que el orden está en estado válido
            validateOrderConsistency(order);
        }
    }
    
    private void validateOrderConsistency(Order order) {
        PaymentStatus paymentStatus = paymentService.getStatus(order.getId());
        InventoryReservation reservation = 
            inventoryService.getReservation(order.getId());
        
        // Máquina de estados válida
        switch (order.getStatus()) {
            case PENDING:
                // No debe haber pago ni reserva
                if (paymentStatus != PaymentStatus.NONE ||
                    reservation != null) {
                    reconcile(order);
                }
                break;
                
            case PAYMENT_PROCESSING:
                // Debe haber pago en curso
                if (paymentStatus != PaymentStatus.PROCESSING) {
                    reconcile(order);
                }
                break;
                
            case CONFIRMED:
                // Debe haber pago completado y reserva activa
                if (paymentStatus != PaymentStatus.COMPLETED ||
                    reservation == null) {
                    reconcile(order);
                }
                break;
                
            case CANCELLED:
                // No debe haber reserva activa
                if (reservation != null && reservation.isActive()) {
                    // Liberar inventario
                    inventoryService.releaseReservation(reservation.getId());
                }
                break;
        }
    }
    
    private void reconcile(Order order) {
        log.warn("Inconsistency detected for order: {}", order.getId());
        // Implementar lógica de reconciliación automática
        // o alertar a equipo de operaciones
    }
}
```

## 3. Monitoreo de Consistency

### 3.1 Métricas de Convergencia
```java
@Component
public class ConsistencyMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordConsistencyChecksum(String serviceId, 
                                         String datasetId,
                                         long checksum) {
        meterRegistry.gauge(
            "consistency.checksum",
            Tags.of(
                "service", serviceId,
                "dataset", datasetId
            ),
            checksum
        );
    }
    
    public void recordEventReplicationDelay(String eventType,
                                           Duration delay) {
        meterRegistry.timer(
            "replication.delay",
            Tags.of("event_type", eventType)
        ).record(delay);
    }
    
    public void recordConflictResolution(String resolution, 
                                        String outcome) {
        meterRegistry.counter(
            "conflicts.resolved",
            Tags.of(
                "strategy", resolution,
                "outcome", outcome
            )
        ).increment();
    }
}
```

### 3.2 Distributed Tracing
```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderCommand cmd,
            HttpServletRequest request) {
        
        // OpenTelemetry: trace ID seguirá la orden
        String traceId = request.getHeader("traceparent");
        
        Order order = orderService.createOrder(cmd);
        
        // Todos los eventos publicados tendrán el mismo trace ID
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}

@Service
public class OrderEventHandler {
    
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // El span padre será el del OrderController
        // Este span será un hijo automáticamente
        paymentService.initiate(event.getOrderId());
    }
}
```

## 4. Casos de Uso y Trade-offs

| Caso de Uso | Consistencia | Justificación | Trade-off |
|------------|--------------|---------------|-----------|
| **Carrito de compras** | Eventual | Actualizaciones frecuentes | Puede ver precios desactualizados |
| **Pago** | Strong | Crítico para negocio | Latencia más alta |
| **Inventario** | Eventual + reconciliación | Balance entre performance y corrección | Puede vender más de lo disponible |
| **Recomendaciones** | Eventual | Datos analíticos no críticos | Recomendaciones desactualizadas |
| **Cuenta de usuario** | Strong en perfil | Seguridad crítica | Bloqueos en actualizaciones frecuentes |

## 5. Testing Eventual Consistency

```java
@SpringBootTest
public class EventualConsistencyTest {
    
    @Test
    public void testOrderConsistencyConverges() 
            throws InterruptedException {
        // 1. Crear orden
        Order order = orderService.createOrder(cmd);
        
        // 2. Inmediatamente después: estado inconsistente es OK
        assertThat(paymentService.getStatus(order.getId()))
            .isNotEqualTo(PaymentStatus.COMPLETED);
        
        // 3. Esperar a que los eventos se procesen
        Thread.sleep(5000); // En prod: usar testcontainers con timeouts
        
        // 4. Verificar convergencia
        assertThat(paymentService.getStatus(order.getId()))
            .isEqualTo(PaymentStatus.COMPLETED);
        
        assertThat(inventoryService.getReservation(order.getId()))
            .isNotNull();
    }
    
    @Test
    public void testPartialFailureRecovery() {
        // Simular fallo en servicio de pago
        paymentService.simulateFailure();
        
        Order order = orderService.createOrder(cmd);
        
        // Estado: inconsistente
        assertThat(order.getStatus()).isEqualTo(Status.PENDING);
        
        // Recuperar servicio
        paymentService.recover();
        
        // Trigger retry
        eventRetryService.retryFailedEvents();
        
        // Verificar convergencia
        awaitUntilAsserted(() -> {
            assertThat(paymentService.getStatus(order.getId()))
                .isEqualTo(PaymentStatus.COMPLETED);
        });
    }
}
```

## Resumen

**Eventual Consistency** es el modelo fundamental de microservicios distribuidos:

1. ✅ **Ventajas**: Disponibilidad, tolerancia a fallos, escalabilidad
2. ❌ **Desventajas**: Complejidad, conflictos, coherencia temporal
3. 🔧 **Soluciones**: Eventos, compensación, reconciliación
4. 📊 **Monitoreo**: Checksums, replication lag, distributed tracing

---

**Siguiente**: [7. Spring Kafka](06-spring-kafka.md)
