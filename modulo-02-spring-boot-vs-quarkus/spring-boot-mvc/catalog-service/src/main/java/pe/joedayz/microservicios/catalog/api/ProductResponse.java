package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.domain.Product;

import java.math.BigDecimal;

public record ProductResponse(
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency());
    }
}
