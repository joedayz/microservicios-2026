package pe.joedayz.microservicios.observability.inventory.web;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

/**
 * Lee {@code X-Tenant-Id} (o el baggage que mando order-service) y lo deja
 * en el span y en el MDC. El default es el tenant de ejemplo del modulo 1.
 */
@Provider
public class TenantContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String HEADER = "X-Tenant-Id";
    static final String DEFAULT_TENANT = "tienda-deportes";
    private static final String SCOPE_KEY = "otel.baggage.scope";

    @Override
    public void filter(ContainerRequestContext request) {
        String tenant = request.getHeaderString(HEADER);
        if (tenant == null || tenant.isBlank()) {
            tenant = Baggage.current().getEntryValue("tenant.id");
        }
        if (tenant == null || tenant.isBlank()) {
            tenant = DEFAULT_TENANT;
        }
        Span.current().setAttribute("tenant.id", tenant);
        MDC.put("tenant.id", tenant);
        Baggage baggage = Baggage.current().toBuilder().put("tenant.id", tenant).build();
        Scope scope = baggage.makeCurrent();
        request.setProperty(SCOPE_KEY, scope);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            response.getHeaders().putSingle("X-Trace-Id", span.getSpanContext().getTraceId());
        }
        Object scope = request.getProperty(SCOPE_KEY);
        if (scope instanceof Scope baggageScope) {
            baggageScope.close();
        }
        MDC.remove("tenant.id");
    }
}
