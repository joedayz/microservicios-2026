package pe.joedayz.microservicios.security.inventory.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import pe.joedayz.microservicios.security.inventory.security.InternalClientPolicyService;
import pe.joedayz.microservicios.security.inventory.service.InventoryService;

/**
 * API interna pensada para tráfico servicio-a-servicio por mTLS.
 * La identidad del servicio la aporta el certificado cliente (capa TLS);
 * X-Client-Id agrega trazabilidad y una validación explícita para la demo.
 */
@Path("/internal/v1/tenants/{tenantId}/inventory")
@Produces(MediaType.APPLICATION_JSON)
public class InternalInventoryResource {

    @Inject
    InventoryService inventoryService;

    @Inject
    InternalClientPolicyService internalClientPolicyService;

    @GET
    @Path("/{sku}")
    public InventoryItemResponse check(@PathParam("tenantId") String tenantId,
                                       @PathParam("sku") String sku,
                                       @QueryParam("region") String region,
                                       @HeaderParam("X-Client-Id") String clientId) {
        internalClientPolicyService.checkInternalClient(clientId);
        return inventoryService.getItem(tenantId, sku);
    }
}
