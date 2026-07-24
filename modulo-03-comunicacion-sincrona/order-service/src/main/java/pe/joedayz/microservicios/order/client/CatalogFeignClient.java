package pe.joedayz.microservicios.order.client;

import pe.joedayz.microservicios.order.tenant.TenantWebFilter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalog-service", url = "${clients.catalog.url}")
public interface CatalogFeignClient {

    @GetMapping("/api/v1/products/{sku}")
    ProductDto getBySku(
            @RequestHeader(TenantWebFilter.TENANT_HEADER) String tenantId,
            @PathVariable("sku") String sku
    );
}
