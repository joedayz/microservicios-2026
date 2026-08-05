package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.domain.Product;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Catalog REST con Virtual Threads.
 *
 * <p>{@link RunOnVirtualThread} indica a Quarkus que el metodo (bloqueante: Panache/JDBC)
 * debe correr en un virtual thread, sin bloquear el event loop de Vert.x.
 * El codigo sigue siendo imperativo — sin Mutiny ni {@code Uni}/{@code Multi}.
 */
@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    @RunOnVirtualThread
    public List<ProductResponse> list() {
        return Product.findByTenant(TenantContext.require()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GET
    @Path("/{sku}")
    @RunOnVirtualThread
    public ProductResponse get(@PathParam("sku") String sku) {
        return Product.findByTenantAndSku(TenantContext.require(), sku)
                .map(ProductResponse::from)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Endpoint didactico: confirma que la peticion corre en un Virtual Thread.
     * Compara con Quarkus clasico (:8083) donde el worker puede no ser virtual.
     */
    @GET
    @Path("/_thread")
    @RunOnVirtualThread
    public Map<String, Object> threadInfo() {
        Thread t = Thread.currentThread();
        return Map.of(
                "name", t.getName(),
                "virtual", t.isVirtual(),
                "framework", "Quarkus REST + @RunOnVirtualThread");
    }
}
