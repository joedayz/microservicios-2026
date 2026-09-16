package pe.joedayz.microservicios.resilience.inventory.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Servicio que envuelve el pricing REST client con las anotaciones
 * de MicroProfile Fault Tolerance.
 *
 * Orden efectivo por decorador (from outermost to innermost):
 *   Bulkhead -> Timeout -> CircuitBreaker -> Retry -> Fallback
 */
@ApplicationScoped
public class PricingClient {

    private static final Logger LOG = Logger.getLogger(PricingClient.class);

    @Inject
    @RestClient
    PricingRestClient rest;

    public record Price(BigDecimal amount, String currency, String source) {}

    @Bulkhead(value = 25, waitingTaskQueue = 10)
    @Timeout(value = 1500, unit = ChronoUnit.MILLIS)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 10_000,
            successThreshold = 3)
    @Retry(
            maxRetries = 3,
            delay = 300,
            jitter = 100,
            retryOn = {java.io.IOException.class,
                       jakarta.ws.rs.WebApplicationException.class,
                       jakarta.ws.rs.ProcessingException.class,
                       org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class})
    @Fallback(fallbackMethod = "fallbackPrice")
    public Price fetch(String sku) {
        var raw = rest.priceOf(sku);
        var amount = new BigDecimal(String.valueOf(raw.getOrDefault("price", "0")));
        var currency = String.valueOf(raw.getOrDefault("currency", "PEN"));
        return new Price(amount, currency, "PRICING_SERVICE");
    }

    @SuppressWarnings("unused")
    Price fallbackPrice(String sku) {
        LOG.warnf("[fallback] price sku=%s -> cached default", sku);
        return new Price(new BigDecimal("149.90"), "PEN", "CACHED_DEFAULT");
    }
}

