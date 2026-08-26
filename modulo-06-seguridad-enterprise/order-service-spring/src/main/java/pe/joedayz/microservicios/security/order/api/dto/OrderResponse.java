package pe.joedayz.microservicios.security.order.api.dto;

public record OrderResponse(
        String id,
        String tenantId,
        String sku,
        int quantity,
        String shippingRegion,
        String status) {
}
