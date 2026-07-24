package pe.joedayz.microservicios.order.api;

import pe.joedayz.microservicios.order.client.ProductDto;

import java.math.BigDecimal;

public record CheckoutResponse(
        String clientStyle,
        String sku,
        String productName,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        boolean stockAvailable,
        int remainingStock,
        String message
) {
    public static CheckoutResponse of(String style, ProductDto product, int quantity,
                                      boolean available, int remaining, String message) {
        return new CheckoutResponse(
                style,
                product.sku(),
                product.name(),
                product.price(),
                product.currency(),
                quantity,
                available,
                remaining,
                message
        );
    }
}
