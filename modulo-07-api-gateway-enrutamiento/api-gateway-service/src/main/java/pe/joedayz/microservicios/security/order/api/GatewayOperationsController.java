package pe.joedayz.microservicios.security.order.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.security.order.config.GatewayTrafficProperties;

@RestController
@RequestMapping("/gateway/admin")
public class GatewayOperationsController {

    private final GatewayTrafficProperties properties;

    public GatewayOperationsController(GatewayTrafficProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/traffic-policies")
    public Map<String, Object> trafficPolicies() {
        return Map.of(
                "springCloudGateway", Map.of(
                        "filters", List.of("Retry", "CircuitBreaker", "RequestRateLimiter", "CORS", "WAF"),
                        "timeouts", Map.of(
                                "default", properties.getTraffic().getDefaultTimeout().toString(),
                                "orders", properties.getTraffic().getOrderTimeout().toString(),
                                "inventory", properties.getTraffic().getInventoryTimeout().toString()),
                        "hedgingDelay", properties.getTraffic().getHedgeDelay().toString()),
                "kongGateway", Map.of(
                        "plugins", List.of("rate-limiting", "cors", "request-termination", "proxy-cache", "bot-detection")),
                "awsApiGateway", Map.of(
                        "features", List.of("usage plans", "throttling", "WAFv2", "Lambda authorizers")),
                "azureApiManagement", Map.of(
                        "features", List.of("policies", "rate-limit-by-key", "JWT validate", "multi-region gateway")));
    }
}
