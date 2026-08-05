package pe.joedayz.microservicios.order.client;

import pe.joedayz.microservicios.order.tenant.TenantWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

/**
 * Cliente reactivo. En este lab usamos {@code block()} solo para comparar
 * estilos de cliente en el mismo flujo de checkout síncrono.
 */
@Component
public class CatalogWebClient {

    private final WebClient webClient;

    public CatalogWebClient(WebClient catalogApiWebClient) {
        this.webClient = catalogApiWebClient;
    }

    public Optional<ProductDto> findBySku(String tenantId, String sku) {
        try {
            ProductDto product = webClient.get()
                    .uri("/api/v1/products/{sku}", sku)
                    .header(TenantWebFilter.TENANT_HEADER, tenantId)
                    .retrieve()
                    .bodyToMono(ProductDto.class)
                    .block();
            return Optional.ofNullable(product);
        } catch (WebClientResponseException.NotFound ex) {
            return Optional.empty();
        }
    }
}
