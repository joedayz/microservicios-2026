package pe.joedayz.microservicios.order.api;

public record CheckoutRequest(String sku, int quantity) {
}
