package pe.joedayz.microservicios.order.client;

import pe.joedayz.microservicios.order.tenant.TenantWebFilter;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class CatalogRestClient {

    private final RestClient restClient;

    public CatalogRestClient(RestClient catalogApiRestClient) {
        this.restClient = catalogApiRestClient;
    }

    public Optional<ProductDto> findBySku(String tenantId, String sku) {
        try {
            ProductDto product = restClient.get()
                    .uri("/api/v1/products/{sku}", sku)
                    .header(TenantWebFilter.TENANT_HEADER, tenantId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    })
                    .body(ProductDto.class);
            return Optional.ofNullable(product);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
