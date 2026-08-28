package pe.joedayz.microservicios.security.order.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class GatewayRouteConfigTest {

    @Test
    void shouldResolveTenantFromHeader() {
        GatewayRouteConfig config = new GatewayRouteConfig();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/gateway/orders/api/v1/tenants/other/orders")
                        .header("X-Tenant-ID", "tienda-deportes")
                        .build());

        String key = config.tenantKeyResolver().resolve(exchange).block();

        assertEquals("tienda-deportes", key);
    }

    @Test
    void shouldResolveTenantFromPathWhenHeaderMissing() {
        GatewayRouteConfig config = new GatewayRouteConfig();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/gateway/orders/api/v1/tenants/libreria-lima/orders").build());

        String key = config.tenantKeyResolver().resolve(exchange).block();

        assertEquals("libreria-lima", key);
    }
}
