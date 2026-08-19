package pe.joedayz.microservicios.catalog.api.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import pe.joedayz.microservicios.catalog.domain.Product;

public record ProductResponse(
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getCurrency());
    }
}
