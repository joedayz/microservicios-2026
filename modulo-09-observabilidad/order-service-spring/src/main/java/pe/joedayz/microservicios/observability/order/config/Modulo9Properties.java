package pe.joedayz.microservicios.observability.order.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "modulo9")
public record Modulo9Properties(Downstream inventory, Downstream flaky) {

    public record Downstream(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
}
