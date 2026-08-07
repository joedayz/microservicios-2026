package pe.joedayz.microservicios.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "catalog-service", url = "${clients.catalog.url}")
public interface CatalogFeignClient {

    @GetMapping("/api/v1/products")
    List<ProductDto> listProducts();

    @GetMapping("/api/v1/products/{sku}")
    ProductDto getBySku(@PathVariable("sku") String sku);
}
