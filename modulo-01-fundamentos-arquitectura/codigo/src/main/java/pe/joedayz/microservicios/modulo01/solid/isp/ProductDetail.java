package pe.joedayz.microservicios.modulo01.solid.isp;

/**
 * Vista de detalle para la web: mas campos que el resumen, pero sin operaciones de admin.
 */
public record ProductDetail(
        String sku,
        String name,
        String price,
        String description,
        String imageUrl) {
}
