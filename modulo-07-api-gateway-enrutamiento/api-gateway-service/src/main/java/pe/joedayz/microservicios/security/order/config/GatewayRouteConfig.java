package pe.joedayz.microservicios.security.order.config;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;

import reactor.core.publisher.Mono;

@Configuration
public class GatewayRouteConfig {

    @Bean
    KeyResolver tenantKeyResolver() {
        return exchange -> Mono.just(resolveTenant(exchange.getRequest()));
    }

    private String resolveTenant(ServerHttpRequest request) {
        String tenantId = request.getHeaders().getFirst("X-Tenant-ID");
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }

        String path = request.getURI().getPath();
        String marker = "/tenants/";
        int markerIndex = path.indexOf(marker);
        if (markerIndex >= 0) {
            int start = markerIndex + marker.length();
            int end = path.indexOf('/', start);
            return end > start ? path.substring(start, end) : path.substring(start);
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress == null ? "anonymous" : remoteAddress.getHostString();
    }
}
