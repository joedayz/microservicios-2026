package pe.joedayz.microservicios.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Clientes HTTP de bajo nivel (infraestructura).
 *
 * <p>Los nombres de bean NO deben coincidir con las clases {@code @Component}
 * {@code CatalogRestClient} / {@code CatalogWebClient} (Spring las registra
 * como beans {@code catalogRestClient} / {@code catalogWebClient}).
 */
@Configuration
public class HttpClientsConfig {

    @Bean
    RestClient catalogApiRestClient(@Value("${clients.catalog.url}") String catalogUrl) {
        return RestClient.builder()
                .baseUrl(catalogUrl)
                .build();
    }

    @Bean
    WebClient catalogApiWebClient(@Value("${clients.catalog.url}") String catalogUrl) {
        return WebClient.builder()
                .baseUrl(catalogUrl)
                .build();
    }
}
