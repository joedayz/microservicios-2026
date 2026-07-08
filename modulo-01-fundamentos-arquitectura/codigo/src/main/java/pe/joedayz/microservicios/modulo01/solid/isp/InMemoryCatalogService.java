package pe.joedayz.microservicios.modulo01.solid.isp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion en memoria que cumple AMBAS interfaces finas (read + admin).
 *
 * <p>En produccion serian dos controllers o dos BFF distintos inyectando solo
 * la interfaz que necesitan — no la God API.
 */
public class InMemoryCatalogService implements CatalogReadApi, CatalogAdminApi {

    private final Map<String, ProductAdminView> products = new LinkedHashMap<>();

    public InMemoryCatalogService() {
        products.put("ZAP-RUN-42", new ProductAdminView(
                "ZAP-RUN-42", "Zapatilla Running Pro", "S/ 300.00",
                "Amortiguacion maxima para corredores", 50, true, "zapatilla-running-pro"));
        products.put("MEDIAS-01", new ProductAdminView(
                "MEDIAS-01", "Medias deportivas pack x3", "S/ 20.00",
                "Pack de 3 pares, talla unica", 200, true, "medias-deportivas"));
        products.put("BORRADOR-99", new ProductAdminView(
                "BORRADOR-99", "Producto en borrador", "S/ 0.00",
                "Aun no publicado", 0, false, ""));
    }

    @Override
    public List<ProductSummary> listPublished() {
        return products.values().stream()
                .filter(ProductAdminView::published)
                .map(p -> new ProductSummary(p.sku(), p.name(), p.price()))
                .toList();
    }

    @Override
    public ProductDetail getBySku(String sku) {
        ProductAdminView p = requirePublished(sku);
        return new ProductDetail(p.sku(), p.name(), p.price(), p.description(),
                "https://cdn.joedayz.pe/img/" + p.sku() + ".jpg");
    }

    @Override
    public List<ProductAdminView> listAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public void create(ProductAdminView product) {
        products.put(product.sku(), product);
    }

    @Override
    public void updateSeo(String sku, String seoSlug) {
        ProductAdminView current = products.get(sku);
        if (current == null) {
            throw new IllegalArgumentException("SKU no encontrado: " + sku);
        }
        products.put(sku, new ProductAdminView(
                current.sku(), current.name(), current.price(), current.description(),
                current.stock(), current.published(), seoSlug));
    }

    @Override
    public void delete(String sku) {
        products.remove(sku);
    }

    private ProductAdminView requirePublished(String sku) {
        ProductAdminView p = products.get(sku);
        if (p == null || !p.published()) {
            throw new IllegalArgumentException("Producto no disponible: " + sku);
        }
        return p;
    }
}
