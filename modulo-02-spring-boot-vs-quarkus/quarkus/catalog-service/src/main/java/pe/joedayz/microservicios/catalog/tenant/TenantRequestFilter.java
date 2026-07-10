package pe.joedayz.microservicios.catalog.tenant;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TenantRequestFilter implements ContainerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("q/")) {
            return;
        }

        String tenantId = requestContext.getHeaderString(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Header " + TENANT_HEADER + " es obligatorio")
                    .build());
            return;
        }

        TenantContext.set(tenantId);
        requestContext.setProperty("tenant.cleanup", true);
    }
}
