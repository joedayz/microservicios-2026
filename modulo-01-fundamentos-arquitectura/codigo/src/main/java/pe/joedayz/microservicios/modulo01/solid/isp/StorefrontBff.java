package pe.joedayz.microservicios.modulo01.solid.isp;

import java.util.List;

/**
 * BFF del storefront: solo depende de {@link CatalogReadApi}.
 *
 * <p>No conoce create/update/delete ni ve borradores. Si el equipo de admin cambia
 * la API de gestion, este BFF NO se ve afectado (ISP en accion).
 */
public class StorefrontBff {

    private final CatalogReadApi catalog;

    public StorefrontBff(CatalogReadApi catalog) {
        this.catalog = catalog;
    }

    /** Endpoint liviano para la app movil: 1 llamada, payload minimo. */
    public List<ProductSummary> homePage() {
        return catalog.listPublished();
    }

    public ProductDetail productPage(String sku) {
        return catalog.getBySku(sku);
    }
}
