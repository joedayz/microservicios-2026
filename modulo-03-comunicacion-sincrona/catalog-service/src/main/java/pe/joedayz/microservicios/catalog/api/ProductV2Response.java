package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.domain.Product;

import java.math.BigDecimal;

/** Respuesta v2: incluye apiVersion y currencyCode (alias didactico). */
public record ProductV2Response(
        String apiVersion,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currencyCode
) {
    public static ProductV2Response from(Product product) {
        return new ProductV2Response(
                "2",
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency()
        );
    }
}
