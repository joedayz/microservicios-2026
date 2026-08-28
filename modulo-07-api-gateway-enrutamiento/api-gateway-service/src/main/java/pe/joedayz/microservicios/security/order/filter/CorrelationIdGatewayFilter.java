package pe.joedayz.microservicios.security.order.filter;

import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdGatewayFilter implements WebFilter, Ordered {

    public static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        String effectiveCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER_NAME, effectiveCorrelationId)
                .build();

        exchange.getResponse().getHeaders().set(HEADER_NAME, effectiveCorrelationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
