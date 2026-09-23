package pe.joedayz.microservicios.observability.inventory.health;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class PricingCircuitBreakerReadiness implements HealthCheck {

    private static final String CB_NAME =
            "pe.joedayz.microservicios.observability.inventory.client.PricingClient/fetch";

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
        boolean up = state == CircuitBreakerState.CLOSED || state == CircuitBreakerState.HALF_OPEN;
        return HealthCheckResponse.named("pricing-circuit-breaker")
                .status(up)
                .withData("state", state.name())
                .build();
    }
}
