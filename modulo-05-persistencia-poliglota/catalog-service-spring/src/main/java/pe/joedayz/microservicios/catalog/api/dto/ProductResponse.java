package pe.joedayz.microservicios.catalog.api.dto;

import java.math.BigDecimal;

import pe.joedayz.microservicios.catalog.domain.Product;

public record ProductResponse(
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency
) {

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
