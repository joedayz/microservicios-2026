package pe.joedayz.microservicios.observability.flaky.web;

import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Tenant-Id";
    static final String DEFAULT_TENANT = "tienda-deportes";

    private final Tracer tracer;

    public TenantContextFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenant = request.getHeader(HEADER);
        if (tenant == null || tenant.isBlank()) {
            var baggage = tracer.getBaggage("tenant.id");
            String fromBaggage = baggage == null ? null : baggage.get();
            tenant = (fromBaggage == null || fromBaggage.isBlank()) ? DEFAULT_TENANT : fromBaggage;
        }
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("tenant.id", tenant);
            response.setHeader("X-Trace-Id", span.context().traceId());
        }
        try (BaggageInScope ignored = tracer.createBaggageInScope("tenant.id", tenant)) {
            chain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/actuator");
    }
}
