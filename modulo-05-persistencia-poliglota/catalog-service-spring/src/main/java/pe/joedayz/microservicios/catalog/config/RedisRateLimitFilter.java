package pe.joedayz.microservicios.catalog.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import pe.joedayz.microservicios.catalog.tenant.TenantContext;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RedisRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = TenantContext.require();
        String clientId = request.getRemoteAddr();
        if (!rateLimitService.isAllowed(tenantId, clientId)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Rate limit excedido para el tenant " + tenantId);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
