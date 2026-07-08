package pe.joedayz.microservicios.modulo01.solid.isp;

import java.util.List;

/**
 * ISP: interfaz FINA para el back-office (BFF Admin / panel interno).
 *
 * <p>Separada de {@link CatalogReadApi}: el admin puede ver borradores, editar SEO
 * y gestionar stock sin contaminar el contrato del storefront.
 */
public interface CatalogAdminApi {

    List<ProductAdminView> listAll();

    void create(ProductAdminView product);

    void updateSeo(String sku, String seoSlug);

    void delete(String sku);
}
