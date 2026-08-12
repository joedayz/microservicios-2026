package pe.joedayz.microservicios.order.api;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.order.api.dto.CreateOrderRequest;
import pe.joedayz.microservicios.order.api.dto.OrderEventView;
import pe.joedayz.microservicios.order.api.dto.OrderResponse;
import pe.joedayz.microservicios.order.domain.Order;
import pe.joedayz.microservicios.order.eventstore.EventStoreService;
import pe.joedayz.microservicios.order.repository.OrderRepository;
import pe.joedayz.microservicios.order.saga.OrderSagaOrchestrator;
import pe.joedayz.microservicios.order.tenant.TenantContext;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;
    private final OrderRepository orderRepository;
    private final EventStoreService eventStore;

    public OrderController(OrderSagaOrchestrator orchestrator,
                            OrderRepository orderRepository,
                            EventStoreService eventStore) {
        this.orchestrator = orchestrator;
        this.orderRepository = orderRepository;
        this.eventStore = eventStore;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        String tenantId = TenantContext.require();
        Order order = orchestrator.createOrder(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        String tenantId = TenantContext.require();
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NoSuchElementException("Order no encontrada: " + orderId));
        return OrderResponse.from(order);
    }

    @GetMapping("/{orderId}/events")
    public List<OrderEventView> getOrderHistory(@PathVariable String orderId) {
        TenantContext.require();
        return eventStore.history(orderId).stream()
                .map(OrderEventView::from)
                .toList();
    }
}
