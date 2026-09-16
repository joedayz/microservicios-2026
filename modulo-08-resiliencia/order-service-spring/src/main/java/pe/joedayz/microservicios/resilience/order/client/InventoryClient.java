package pe.joedayz.microservicios.resilience.order.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente del inventory-service-quarkus.
 *
 * Orden de decoradores (from outermost to innermost):
 *   Bulkhead -> TimeLimiter -> CircuitBreaker -> Retry -> Fallback
 *
 * Esto asegura que:
 *  - Bulkhead limite la concurrencia antes de gastar recursos.
 *  - TimeLimiter aborta llamadas colgadas.
 *  - CircuitBreaker corta trafico cuando la tasa de fallos sube.
 *  - Retry solo aplica dentro de un breaker cerrado.
 *  - Fallback devuelve una respuesta degradada.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private static final String INSTANCE = "inventoryClient";

    private final RestClient http;

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient http) {
        this.http = http;
    }

    @Bulkhead(name = INSTANCE, type = Bulkhead.Type.SEMAPHORE)
    @TimeLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "reserveFallback")
    @Retry(name = INSTANCE)
    public CompletableFuture<Map<String, Object>> reserve(String sku, int qty, String orderId) {
        return CompletableFuture.supplyAsync(() -> http.post()
                .uri("/api/v1/inventory/reserve")
                .body(Map.of("sku", sku, "qty", qty, "orderId", orderId))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Map<String, Object>> reserveFallback(
            String sku, int qty, String orderId, Throwable ex) {
        log.warn("[fallback] reserve sku={} orderId={} cause={}", sku, orderId, ex.toString());
        return CompletableFuture.completedFuture(Map.of(
                "sku", sku,
                "qty", qty,
                "status", "DEGRADED",
                "reason", ex.getClass().getSimpleName()));
    }
}

