package pe.joedayz.microservicios.security.order.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import pe.joedayz.microservicios.security.order.api.SecurityDemoController.OrderReportLine;
import pe.joedayz.microservicios.security.order.api.dto.CreateOrderRequest;
import pe.joedayz.microservicios.security.order.api.dto.OrderResponse;

@Service
public class OrderService {

    private final AtomicInteger sequence = new AtomicInteger(2000);
    private final CopyOnWriteArrayList<OrderResponse> orders = new CopyOnWriteArrayList<>(List.of(
            new OrderResponse("ORD-1001", "tienda-deportes", "ZAP-RUN-42", 1, "PE", "AUTHORIZED"),
            new OrderResponse("ORD-1002", "libreria-lima", "LIB-DDD-01", 3, "PE", "AUTHORIZED")));

    public List<OrderResponse> listOrders(String tenantId) {
        return orders.stream()
                .filter(order -> order.tenantId().equalsIgnoreCase(tenantId))
                .toList();
    }

    public OrderResponse createOrder(String tenantId, CreateOrderRequest request) {
        OrderResponse order = new OrderResponse(
                "ORD-" + sequence.incrementAndGet(),
                tenantId,
                request.sku(),
                request.quantity(),
                request.shippingRegion(),
                "AUTHORIZED");
        orders.add(order);
        return order;
    }

    public List<OrderReportLine> buildReport() {
        return orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(OrderResponse::tenantId, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new OrderReportLine(entry.getKey(), entry.getValue()))
                .toList();
    }
}
