package pe.joedayz.microservicios.resilience.inventory.health;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness check dedicado al Circuit Breaker de pricing.
 * Si el CB esta OPEN el pod se marca NOT READY, Kubernetes deja de mandarle
 * trafico pero NO lo reinicia (liveness sigue arriba).
 *
 * El nombre del CB coincide con el metodo anotado con @CircuitBreaker:
 *   <fully-qualified-classname>/<methodName>
 */
@Readiness
@ApplicationScoped
public class PricingCircuitBreakerReadiness implements HealthCheck {

    private static final String CB_NAME =
            "pe.joedayz.microservicios.resilience.inventory.client.PricingClient/fetch";

    @Inject
    CircuitBreakerMaintenance cbm;

    @Override
    public HealthCheckResponse call() {
        CircuitBreakerState state;
        try {
            state = cbm.currentState(CB_NAME);
        } catch (Exception ex) {
            return HealthCheckResponse.named("pricing-circuit-breaker")
                    .up()
                    .withData("state", "UNKNOWN")
                    .withData("note", "CB no inicializado aun")
                    .build();
        }
        boolean up = state == CircuitBreakerState.CLOSED
                || state == CircuitBreakerState.HALF_OPEN;
        return HealthCheckResponse.named("pricing-circuit-breaker")
                .status(up)
                .withData("state", state.name())
                .build();
    }
}

