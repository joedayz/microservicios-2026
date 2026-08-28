package pe.joedayz.microservicios.security.order.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/fallback/orders")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> orderFallback() {
        return Map.of(
                "service", "order-service",
                "status", "degraded",
                "message", "Circuit breaker abierto: responde el fallback del gateway.",
                "timestamp", Instant.now().toString());
    }

    @GetMapping("/fallback/inventory")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> inventoryFallback() {
        return Map.of(
                "service", "inventory-service",
                "status", "degraded",
                "message", "Circuit breaker abierto: responde el fallback del gateway.",
                "timestamp", Instant.now().toString());
    }
}
