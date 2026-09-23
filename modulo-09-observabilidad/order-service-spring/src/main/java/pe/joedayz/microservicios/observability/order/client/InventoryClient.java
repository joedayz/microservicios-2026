package pe.joedayz.microservicios.observability.order.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reserva stock en inventory-service-quarkus.
 *
 * <p>El fan-out usa virtual threads (el mismo recurso del modulo 1). El
 * {@link ContextSnapshot} copia traceId, spanId y el baggage {@code tenant.id}
 * al hilo nuevo: sin el {@code setThreadLocals}, el {@code traceparent} nace de cero y Tempo parte
 * la orden en dos trazas.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private static final String INSTANCE = "inventoryClient";

    private final RestClient http;
    private final ExecutorService executor;
    private final ContextSnapshotFactory snapshots = ContextSnapshotFactory.builder().build();

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient http, ExecutorService orderCallsExecutor) {
        this.http = http;
        this.executor = orderCallsExecutor;
    }

    @Bulkhead(name = INSTANCE, type = Bulkhead.Type.SEMAPHORE)
    @TimeLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "reserveFallback")
    @Retry(name = INSTANCE)
    public CompletableFuture<Map<String, Object>> reserve(String sku, int qty, String orderId) {
        ContextSnapshot snapshot = snapshots.captureAll();
        return CompletableFuture.supplyAsync(() -> {
            try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
                try {
                    return http.post()
                            .uri("/api/v1/inventory/reserve")
                            .body(Map.of("sku", sku, "qty", qty, "orderId", orderId))
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                } catch (RuntimeException ex) {
                    log.warn("fallback reserve sku={} orderId={} cause={}", sku, orderId, ex.toString());
                    throw ex;
                }
            }
        }, executor);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Map<String, Object>> reserveFallback(
            String sku, int qty, String orderId, Throwable ex) {
        if (ex instanceof CallNotPermittedException || org.slf4j.MDC.get("traceId") != null) {
            log.warn("fallback reserve sku={} orderId={} cause={}", sku, orderId, ex.toString());
        }
        return CompletableFuture.completedFuture(Map.of(
                "sku", sku,
                "qty", qty,
                "status", "DEGRADED",
                "reason", ex.getClass().getSimpleName()));
    }
}
