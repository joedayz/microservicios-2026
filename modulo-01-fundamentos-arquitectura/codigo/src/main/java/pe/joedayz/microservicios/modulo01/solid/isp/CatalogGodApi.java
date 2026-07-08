package pe.joedayz.microservicios.modulo01.solid.isp;

import java.util.List;

/**
 * ANTI-PATRON (viola ISP): una interfaz gorda que mezcla lectura de storefront,
 * detalle web y operaciones de administracion.
 *
 * <p>Problema: la app movil depende de metodos que no usa ({@code createProduct},
 * {@code updateSeo}, {@code deleteProduct}). Cualquier cambio en admin obliga a
 * redeployar/revisar consumidores que solo leen catalogo.
 *
 * <p>En microservicios esto se convierte en un "God API" que sirve a todos los
 * frontends con el mismo contrato — mal compromiso para movil, web y admin.
 */
public interface CatalogGodApi {

    // --- Lo que necesita el storefront (movil/web) ---
    List<ProductSummary> listPublished();

    ProductDetail getBySku(String sku);

    // --- Lo que SOLO necesita el admin (pero todos dependen de esta interfaz) ---
    List<ProductAdminView> listAllIncludingDrafts();

    void createProduct(ProductAdminView product);

    void updateSeo(String sku, String seoSlug);

    void deleteProduct(String sku);
}
