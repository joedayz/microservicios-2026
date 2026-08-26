package pe.joedayz.microservicios.security.inventory.api;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import pe.joedayz.microservicios.security.inventory.security.AccessPolicyService;
import pe.joedayz.microservicios.security.inventory.service.InventoryService;

/**
 * API pública protegida con OIDC.
 * RBAC: @RolesAllowed decide qué rol puede entrar.
 * ABAC: AccessPolicyService valida tenant y región del token.
 */
@Path("/api/v1/tenants/{tenantId}/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @Inject
    InventoryService inventoryService;

    @Inject
    AccessPolicyService accessPolicyService;

    @GET
    @RolesAllowed({"inventory_viewer", "inventory_manager", "inventory_admin"})
    public List<InventoryItemResponse> list(@PathParam("tenantId") String tenantId) {
        accessPolicyService.checkTenantAccess(tenantId);
        return inventoryService.listItems(tenantId);
    }

    @GET
    @Path("/{sku}")
    @RolesAllowed({"inventory_viewer", "inventory_manager", "inventory_admin"})
    public InventoryItemResponse get(@PathParam("tenantId") String tenantId,
                                     @PathParam("sku") String sku,
                                     @QueryParam("region") String region) {
        accessPolicyService.checkTenantAccess(tenantId);
        if (region != null && !region.isBlank()) {
            accessPolicyService.checkRegionAccess(region);
        }
        return inventoryService.getItem(tenantId, sku);
    }

    @POST
    @Path("/{sku}/reserve")
    @RolesAllowed({"inventory_manager", "inventory_admin"})
    public InventoryItemResponse reserve(@PathParam("tenantId") String tenantId,
                                         @PathParam("sku") String sku,
                                         @Valid ReserveStockRequest request) {
        accessPolicyService.checkTenantAccess(tenantId);
        accessPolicyService.checkRegionAccess(request.region());
        return inventoryService.reserve(tenantId, sku, request.quantity());
    }
}
