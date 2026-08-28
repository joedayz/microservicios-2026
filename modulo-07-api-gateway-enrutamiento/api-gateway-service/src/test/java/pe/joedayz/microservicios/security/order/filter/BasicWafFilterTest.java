package pe.joedayz.microservicios.security.order.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import pe.joedayz.microservicios.security.order.config.GatewayTrafficProperties;
import reactor.core.publisher.Mono;

class BasicWafFilterTest {

    @Test
    void shouldBlockKnownScannerUserAgent() {
        GatewayTrafficProperties properties = new GatewayTrafficProperties();
        properties.getWaf().setBlockedUserAgents(java.util.List.of("sqlmap"));
        properties.getWaf().setBlockedPathFragments(java.util.List.of("../"));

        BasicWafFilter filter = new BasicWafFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/gateway/orders/api/v1/tenants/demo/orders")
                        .header("User-Agent", "sqlmap/1.0")
                        .build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertEquals("blocked", exchange.getResponse().getHeaders().getFirst("X-WAF-Action"));
    }
}
