package pe.joedayz.microservicios.modulo01.solid.isp;

/**
 * Vista liviana de producto para storefront / app movil.
 * Solo los campos que el cliente necesita ver en un listado.
 */
public record ProductSummary(String sku, String name, String price) {
}
