package pe.joedayz.microservicios.observability.inventory.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

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
