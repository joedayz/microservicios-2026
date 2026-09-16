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
 * Cliente del flaky-downstream-service usado para calcular riesgo.
 * Este es el downstream MENOS confiable, por eso tiene una config de CB mas agresiva
 * definida en application.yml bajo la instancia {@code riskClient}.
 */
@Component
public class RiskClient {

    private static final Logger log = LoggerFactory.getLogger(RiskClient.class);
    private static final String INSTANCE = "riskClient";

    private final RestClient http;

    public RiskClient(@Qualifier("flakyRestClient") RestClient http) {
        this.http = http;
    }

    @Bulkhead(name = INSTANCE, type = Bulkhead.Type.SEMAPHORE)
    @TimeLimiter(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "riskFallback")
    @Retry(name = INSTANCE)
    public CompletableFuture<Map<String, Object>> score(String orderId) {
        return CompletableFuture.supplyAsync(() -> http.get()
                .uri("/flaky/risk/{orderId}", orderId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Map<String, Object>> riskFallback(String orderId, Throwable ex) {
        log.warn("[fallback] risk orderId={} cause={}", orderId, ex.toString());
        return CompletableFuture.completedFuture(Map.of(
                "orderId", orderId,
                "score", 50,
                "decision", "MANUAL_REVIEW",
                "reason", ex.getClass().getSimpleName()));
    }
}

