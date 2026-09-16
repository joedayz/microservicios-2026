package pe.joedayz.microservicios.resilience.inventory.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import pe.joedayz.microservicios.resilience.inventory.client.PricingClient;

@Path("/api/v1/inventory")
@ApplicationScoped
public class InventoryResource {

    @Inject
    PricingClient pricing;

    @POST
    @Path("/reserve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReserveResponse reserve(@Valid ReserveRequest req) {
        PricingClient.Price price = pricing.fetch(req.sku());
        return new ReserveResponse(
                req.orderId(),
                req.sku(),
                req.qty(),
                "RESERVED",
                price.amount(),
                price.currency(),
                price.source());
    }
}

