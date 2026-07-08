package pe.joedayz.microservicios.modulo01.solid.isp;

import java.util.List;

/**
 * BFF del panel admin: solo depende de {@link CatalogAdminApi}.
 *
 * <p>Puede listar borradores, editar SEO y eliminar productos sin arrastrar
 * esa complejidad al storefront.
 */
public class AdminBff {

    private final CatalogAdminApi catalog;

    public AdminBff(CatalogAdminApi catalog) {
        this.catalog = catalog;
    }

    public List<ProductAdminView> dashboard() {
        return catalog.listAll();
    }

    public void publishProduct(ProductAdminView product) {
        catalog.create(product);
    }

    public void optimizeSeo(String sku, String slug) {
        catalog.updateSeo(sku, slug);
    }
}
