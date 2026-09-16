package pe.joedayz.microservicios.resilience.order.api;

import java.util.Map;

public record OrderResponse(
        String orderId,
        String sku,
        int qty,
        String status,
        Map<String, Object> inventory,
        Map<String, Object> risk,
        String degradation) {
}

