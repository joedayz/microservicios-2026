package pe.joedayz.microservicios.resilience.inventory.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

/**
 * Startup check: solo se completa despues de que la aplicacion ha calentado.
 * Kubernetes lo usa para postergar liveness/readiness durante el arranque.
 */
@Startup
@ApplicationScoped
public class WarmupStartupCheck implements HealthCheck {

    private final long readyAt = System.currentTimeMillis() + 3_000;

    @Override
    public HealthCheckResponse call() {
        boolean up = System.currentTimeMillis() >= readyAt;
        return HealthCheckResponse.named("warmup")
                .status(up)
                .withData("readyAt", readyAt)
                .build();
    }
}

