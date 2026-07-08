package pe.joedayz.microservicios.modulo01.solid.isp;

/**
 * Demo ISP: interfaces finas vs God API.
 *
 * <pre>
 *   mvn -q compile exec:java -Dexec.mainClass=pe.joedayz.microservicios.modulo01.solid.isp.Main
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== ISP: CatalogReadApi vs CatalogAdminApi ===\n");

        InMemoryCatalogService catalog = new InMemoryCatalogService();

        // Cada BFF solo recibe la interfaz que necesita (ISP).
        StorefrontBff storefront = new StorefrontBff(catalog);
        AdminBff admin = new AdminBff(catalog);

        System.out.println("--- Storefront (solo CatalogReadApi) ---");
        System.out.println("Productos publicados: " + storefront.homePage().size());
        storefront.homePage().forEach(p ->
                System.out.println("  • " + p.sku() + " — " + p.name() + " (" + p.price() + ")"));
        System.out.println("Detalle: " + storefront.productPage("ZAP-RUN-42").description());

        System.out.println("\n--- Admin (solo CatalogAdminApi) ---");
        System.out.println("Total en catalogo (incl. borradores): " + admin.dashboard().size());
        admin.dashboard().stream()
                .filter(p -> !p.published())
                .forEach(p -> System.out.println("  • BORRADOR: " + p.sku() + " — " + p.name()));

        System.out.println("\n--- Anti-patron CatalogGodApi ---");
        System.out.println("La app movil dependeria de createProduct(), updateSeo(), deleteProduct()");
        System.out.println("aunque nunca los use. Solucion: separar en CatalogReadApi + CatalogAdminApi.");
    }
}
