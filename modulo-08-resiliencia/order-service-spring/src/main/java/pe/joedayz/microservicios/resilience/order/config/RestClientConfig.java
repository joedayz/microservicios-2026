package pe.joedayz.microservicios.resilience.order.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("inventoryRestClient")
    RestClient inventoryRestClient(Modulo8Properties props) {
        return build(props.inventory());
    }

    @Bean("flakyRestClient")
    RestClient flakyRestClient(Modulo8Properties props) {
        return build(props.flaky());
    }

    private RestClient build(Modulo8Properties.Downstream ds) {
        Duration connect = ds.connectTimeout() == null ? Duration.ofSeconds(2) : ds.connectTimeout();
        Duration read = ds.readTimeout() == null ? Duration.ofSeconds(2) : ds.readTimeout();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connect)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(read);

        return RestClient.builder()
                .baseUrl(ds.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
