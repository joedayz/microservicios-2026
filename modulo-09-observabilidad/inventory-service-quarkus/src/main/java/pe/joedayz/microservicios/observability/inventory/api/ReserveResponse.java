package pe.joedayz.microservicios.observability.inventory.api;

import java.math.BigDecimal;

public record ReserveResponse(
        String orderId,
        String sku,
        int qty,
        String status,
        BigDecimal unitPrice,
        String currency,
        String priceSource) {
}
