package pe.joedayz.microservicios.order.client;

import java.math.BigDecimal;

public record ProductDto(
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency
) {
}
