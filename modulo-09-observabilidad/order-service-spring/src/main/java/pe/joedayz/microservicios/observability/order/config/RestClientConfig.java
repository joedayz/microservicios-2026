package pe.joedayz.microservicios.observability.order.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.MDC;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Los clientes HTTP salen del {@link RestClient.Builder} auto-configurado.
 * Ese builder ya lleva el interceptor de observacion: sin el, el header
 * {@code traceparent} no viaja y Tempo muestra tres trazas sueltas en vez
 * de una sola traza del checkout.
 *
 * <p>En Spring Boot 4 el builder es un prototipo que se personaliza con
 * {@link RestClientCustomizer}; no se inyecta el builder directamente.
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClientCustomizer tenantAndTimeouts(Modulo9Properties props) {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            String tenant = MDC.get("tenant.id");
            if (tenant != null && !tenant.isBlank()) {
                request.getHeaders().set("X-Tenant-Id", tenant);
            }
            return execution.execute(request, body);
        });
    }

    @Bean(destroyMethod = "close")
    ExecutorService orderCallsExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean("inventoryRestClient")
    RestClient inventoryRestClient(Modulo9Properties props, RestClient.Builder builder) {
        return http(props.inventory(), builder);
    }

    @Bean("flakyRestClient")
    RestClient flakyRestClient(Modulo9Properties props, RestClient.Builder builder) {
        return http(props.flaky(), builder);
    }

    private RestClient http(Modulo9Properties.Downstream ds, RestClient.Builder builder) {
        Duration connect = ds.connectTimeout() == null ? Duration.ofSeconds(2) : ds.connectTimeout();
        Duration read = ds.readTimeout() == null ? Duration.ofSeconds(2) : ds.readTimeout();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connect).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(read);
        return builder.baseUrl(ds.baseUrl()).requestFactory(factory).build();
    }
}
