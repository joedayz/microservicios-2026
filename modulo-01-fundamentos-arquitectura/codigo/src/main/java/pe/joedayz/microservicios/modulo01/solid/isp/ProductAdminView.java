package pe.joedayz.microservicios.modulo01.solid.isp;

/**
 * Vista completa para el back-office: incluye campos internos que el cliente nunca debe ver.
 */
public record ProductAdminView(
        String sku,
        String name,
        String price,
        String description,
        int stock,
        boolean published,
        String seoSlug) {
}
