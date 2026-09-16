package pe.joedayz.microservicios.resilience.order.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator dedicado que marca readiness DOWN si el CB de inventory
 * esta OPEN. Sirve para que Kubernetes retire el pod del Service (readinessProbe)
 * pero SIN reiniciarlo (livenessProbe sigue arriba).
 */
@Component("downstreamCircuitBreakers")
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry registry;

    public CircuitBreakerHealthIndicator(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        Map<String, String> states = new LinkedHashMap<>();
        boolean anyOpen = false;
        for (CircuitBreaker cb : registry.getAllCircuitBreakers()) {
            states.put(cb.getName(), cb.getState().name());
            if (cb.getState() == CircuitBreaker.State.OPEN
                    || cb.getState() == CircuitBreaker.State.FORCED_OPEN) {
                anyOpen = true;
            }
        }
        Health.Builder builder = anyOpen ? Health.outOfService() : Health.up();
        return builder.withDetails(states).build();
    }
}

