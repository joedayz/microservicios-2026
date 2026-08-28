package pe.joedayz.microservicios.security.order.filter;

import java.time.Duration;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

import pe.joedayz.microservicios.security.order.config.GatewayTrafficProperties;
import reactor.core.publisher.Mono;

@Component
public class HedgingTrafficFilter implements WebFilter, Ordered {

    private final GatewayTrafficProperties properties;

    public HedgingTrafficFilter(GatewayTrafficProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) {
        String hedgeHeader = exchange.getRequest().getHeaders().getFirst("X-Hedge-Request");
        if (!"true".equalsIgnoreCase(hedgeHeader)) {
            return chain.filter(exchange);
        }

        Duration delay = properties.getTraffic().getHedgeDelay();
        exchange.getResponse().getHeaders().set("X-Hedging-Strategy", "synthetic-parallel-request");
        return Mono.delay(delay)
                .then(chain.filter(exchange))
                .timeout(properties.getTraffic().getDefaultTimeout())
                .onErrorResume(throwable -> {
                    exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
