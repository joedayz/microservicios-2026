package pe.joedayz.microservicios.security.order.filter;

import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

import pe.joedayz.microservicios.security.order.config.GatewayTrafficProperties;
import reactor.core.publisher.Mono;

@Component
public class BasicWafFilter implements WebFilter, Ordered {

    private final GatewayTrafficProperties properties;

    public BasicWafFilter(GatewayTrafficProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) {
        if (!properties.getWaf().isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath().toLowerCase(Locale.ROOT);
        String query = request.getURI().getRawQuery();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String lowerUserAgent = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        String lowerQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);

        boolean blocked = properties.getWaf().getBlockedPathFragments().stream()
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .anyMatch(fragment -> path.contains(fragment) || lowerQuery.contains(fragment));
        blocked = blocked || properties.getWaf().getBlockedUserAgents().stream()
                .map(agent -> agent.toLowerCase(Locale.ROOT))
                .anyMatch(lowerUserAgent::contains);

        if (!blocked) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().set("X-WAF-Action", "blocked");
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
