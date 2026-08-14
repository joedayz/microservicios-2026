package pe.joedayz.microservicios.order.saga;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import pe.joedayz.microservicios.order.api.dto.CreateOrderRequest;
import pe.joedayz.microservicios.order.domain.Order;
import pe.joedayz.microservicios.order.events.OrderConfirmedEvent;
import pe.joedayz.microservicios.order.events.OrderCreatedEvent;
import pe.joedayz.microservicios.order.events.OrderFailedEvent;
import pe.joedayz.microservicios.order.events.ReserveStockCommand;
import pe.joedayz.microservicios.order.events.StockReservationFailedEvent;
import pe.joedayz.microservicios.order.events.StockReservedEvent;
import pe.joedayz.microservicios.order.eventstore.EventStoreService;
import pe.joedayz.microservicios.order.repository.OrderRepository;

/**
 * Saga Orchestrator: Order Service coordina el flujo de checkout.
 *
 * <pre>
 * 1. createOrder()          → persiste Order(PENDING) + OrderCreatedEvent en el
 *                             event store, publica ReserveStockCommand.
 * 2. handleStockReserved()  → Catalog Service confirmó reserva (topic
 *                             "stock-reserved"); marca Order CONFIRMED.
 *                             Este mismo evento también lo consume Inventory
 *                             Service para decrementar stock físico (choreography
 *                             híbrida: un evento, dos consumer groups).
 * 3. handleReservationFailed() → Catalog Service no tuvo stock; marca Order FAILED.
 * </pre>
 *
 * Para simplificar el ejemplo didáctico, la saga tiene 2 pasos (reserva de stock).
 * Agregar un tercer paso (pago) sigue el mismo patrón: nuevo comando +
 * nuevo listener de reply + nuevo estado intermedio.
 */
@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final EventStoreService eventStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                  EventStoreService eventStore,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(String tenantId, CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, tenantId, request.customerId(), request.sku(), request.quantity());
        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, tenantId, request.customerId(),
                request.sku(), request.quantity(), System.currentTimeMillis());
        eventStore.append(orderId, tenantId, event);

        log.info("Order {} creada (tenant={}), iniciando saga: ReserveStockCommand", orderId, tenantId);
        kafkaTemplate.send("reserve-stock-command", orderId,
                new ReserveStockCommand(orderId, tenantId, request.sku(), request.quantity()));

        return order;
    }

    @KafkaListener(topics = "stock-reserved", groupId = "order-service")
    @Transactional
    public void handleStockReserved(String payload) throws Exception {
        StockReservedEvent event = objectMapper.readValue(payload, StockReservedEvent.class);
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new NoSuchElementException("Order no encontrada: " + event.orderId()));

        order.markStockReserved();
        eventStore.append(order.getId(), order.getTenantId(), event);

        // En esta versión simplificada, reservar stock es el único paso de la saga.
        order.markConfirmed();
        OrderConfirmedEvent confirmed = new OrderConfirmedEvent(order.getId(), order.getTenantId(),
                System.currentTimeMillis());
        eventStore.append(order.getId(), order.getTenantId(), confirmed);
        orderRepository.save(order);

        log.info("Order {} CONFIRMED", order.getId());
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "order-service")
    @Transactional
    public void handleReservationFailed(String payload) throws Exception {
        StockReservationFailedEvent event = objectMapper.readValue(payload, StockReservationFailedEvent.class);
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new NoSuchElementException("Order no encontrada: " + event.orderId()));

        order.markFailed();
        orderRepository.save(order);

        OrderFailedEvent failed = new OrderFailedEvent(order.getId(), order.getTenantId(),
                event.reason(), System.currentTimeMillis());
        eventStore.append(order.getId(), order.getTenantId(), failed);

        log.warn("Order {} FAILED: {}", order.getId(), event.reason());
    }
}
