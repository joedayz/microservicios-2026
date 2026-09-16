package pe.joedayz.microservicios.resilience.inventory.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente MicroProfile REST hacia el flaky-downstream-service.
 * La configuracion del endpoint vive en application.properties bajo
 * quarkus.rest-client.pricing-client.url.
 */
@Path("/flaky")
@RegisterRestClient(configKey = "pricing-client")
public interface PricingRestClient {

    @GET
    @Path("/price/{sku}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> priceOf(@PathParam("sku") String sku);
}

