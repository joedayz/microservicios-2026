package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.domain.Product;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    public List<ProductResponse> list() {
        return Product.findByTenant(TenantContext.require()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GET
    @Path("/{sku}")
    public ProductResponse get(@PathParam("sku") String sku) {
        return Product.findByTenantAndSku(TenantContext.require(), sku)
                .map(ProductResponse::from)
                .orElseThrow(NotFoundException::new);
    }
}
