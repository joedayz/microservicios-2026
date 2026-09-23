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
 * Scoring de riesgo contra el flaky-downstream. Corre en paralelo a la reserva
 * de stock y comparte el mismo traceId gracias al {@link ContextSnapshot}.
 */
@Component
public class RiskClient {

    private static final Logger log = LoggerFactory.getLogger(RiskClient.class);
    private static final String INSTANCE = "riskClient";

    private final RestClient http;
    private final ExecutorService executor;
    private final ContextSnapshotFactory snapshots = ContextSnapshotFactory.builder().build();

    public RiskClient(@Qualifier("flakyRestClient") RestClient http, ExecutorService orderCallsExecutor) {
        this.http = http;
        this.executor = orderCallsExecutor;
    }

    @Bulkhead(name = INSTANCE, type = Bulkhead.Type.SEMAPHORE)
    @TimeLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "riskFallback")
    @Retry(name = INSTANCE)
    public CompletableFuture<Map<String, Object>> score(String orderId) {
        ContextSnapshot snapshot = snapshots.captureAll();
        return CompletableFuture.supplyAsync(() -> {
            try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
                try {
                    return http.get()
                            .uri("/flaky/risk/{orderId}", orderId)
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                } catch (RuntimeException ex) {
                    log.warn("fallback risk orderId={} cause={}", orderId, ex.toString());
                    throw ex;
                }
            }
        }, executor);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Map<String, Object>> riskFallback(String orderId, Throwable ex) {
        if (ex instanceof CallNotPermittedException || org.slf4j.MDC.get("traceId") != null) {
            log.warn("fallback risk orderId={} cause={}", orderId, ex.toString());
        }
        return CompletableFuture.completedFuture(Map.of(
                "orderId", orderId,
                "score", 50,
                "decision", "MANUAL_REVIEW",
                "reason", ex.getClass().getSimpleName()));
    }
}
