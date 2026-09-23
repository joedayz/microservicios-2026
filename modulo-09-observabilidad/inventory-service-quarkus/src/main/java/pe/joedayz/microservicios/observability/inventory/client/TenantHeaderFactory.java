package pe.joedayz.microservicios.observability.inventory.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.MDC;

/**
 * Reenvia el tenant al pricing. El {@code traceparent} lo inyecta
 * quarkus-opentelemetry; este factory solo se ocupa del header de negocio.
 */
@ApplicationScoped
public class TenantHeaderFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incoming,
            MultivaluedMap<String, String> outgoing) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (outgoing != null) {
            headers.putAll(outgoing);
        }
        String tenant = incoming == null ? null : incoming.getFirst("X-Tenant-Id");
        if (tenant == null || tenant.isBlank()) {
            Object fromMdc = MDC.get("tenant.id");
            tenant = fromMdc == null ? null : fromMdc.toString();
        }
        if (tenant != null && !tenant.isBlank()) {
            headers.putSingle("X-Tenant-Id", tenant);
        }
        return headers;
    }
}
