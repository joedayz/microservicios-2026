package pe.joedayz.microservicios.inventory.api;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import pe.joedayz.microservicios.inventory.service.InventoryService;

@Path("/api/v1/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @Inject
    InventoryService inventoryService;

    @GET
    public List<InventoryResponse> list() {
        return inventoryService.listItems();
    }

    @GET
    @Path("/{sku}")
    public InventoryResponse get(@PathParam("sku") String sku) {
        return inventoryService.getItem(sku);
    }

    @POST
    @Path("/{sku}/reserve")
    public InventoryResponse reserve(@PathParam("sku") String sku, @Valid ReserveStockRequest request) {
        return inventoryService.reserve(sku, request.quantity());
    }
}
