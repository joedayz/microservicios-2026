package pe.joedayz.microservicios.observability.inventory.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import pe.joedayz.microservicios.observability.inventory.client.PricingClient;

@Path("/api/v1/inventory")
@ApplicationScoped
public class InventoryResource {

    private static final Logger LOG = Logger.getLogger(InventoryResource.class);

    @Inject
    PricingClient pricing;

    @POST
    @Path("/reserve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReserveResponse reserve(@Valid ReserveRequest req) {
        PricingClient.Price price = pricing.fetch(req.sku());
        LOG.infof("reserva orderId=%s sku=%s qty=%d priceSource=%s tenant=%s",
                req.orderId(), req.sku(), req.qty(), price.source(), MDC.get("tenant.id"));
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
