package pe.joedayz.microservicios.order.api.dto;

public record CreateOrderRequest(
        String customerId,
        String sku,
        int quantity
) {
}
