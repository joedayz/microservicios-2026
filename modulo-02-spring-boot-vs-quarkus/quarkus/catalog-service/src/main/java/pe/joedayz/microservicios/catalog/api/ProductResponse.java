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
                product.sku,
                product.name,
                product.description,
                product.price,
                product.currency);
    }
}
