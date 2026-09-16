package pe.joedayz.microservicios.resilience.order.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "modulo8")
public record Modulo8Properties(
        Downstream inventory,
        Downstream flaky) {

    public record Downstream(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
}

