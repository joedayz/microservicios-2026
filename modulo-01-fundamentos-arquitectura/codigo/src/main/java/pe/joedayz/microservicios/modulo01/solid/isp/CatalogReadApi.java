package pe.joedayz.microservicios.modulo01.solid.isp;

import java.util.List;

/**
 * ISP: interfaz FINA para consumidores de lectura (storefront, BFF movil, BFF web).
 *
 * <p>Solo expone lo que un cliente final necesita: listar y ver detalle.
 * No incluye create/update/delete ni campos internos de admin.
 *
 * <p>En el curso esto justifica el patron BFF: cada frontend consume una API
 * acotada a su caso de uso (ver {@link StorefrontBff}).
 */
public interface CatalogReadApi {

    List<ProductSummary> listPublished();

    ProductDetail getBySku(String sku);
}
