package pe.joedayz.microservicios.observability.inventory.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/flaky")
@RegisterRestClient(configKey = "pricing-client")
@RegisterClientHeaders(TenantHeaderFactory.class)
public interface PricingRestClient {

    @GET
    @Path("/price/{sku}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> priceOf(@PathParam("sku") String sku);
}
