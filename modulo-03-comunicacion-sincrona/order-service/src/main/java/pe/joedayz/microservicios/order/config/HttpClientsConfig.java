package pe.joedayz.microservicios.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    RestClient catalogRestClient(@Value("${clients.catalog.url}") String catalogUrl) {
        return RestClient.builder()
                .baseUrl(catalogUrl)
                .build();
    }

    @Bean
    WebClient catalogWebClient(@Value("${clients.catalog.url}") String catalogUrl) {
        return WebClient.builder()
                .baseUrl(catalogUrl)
                .build();
    }
}
