package pe.joedayz.microservicios.order.api;

import pe.joedayz.microservicios.inventory.v1.CheckStockResponse;
import pe.joedayz.microservicios.order.client.CatalogFeignClient;
import pe.joedayz.microservicios.order.client.CatalogRestClient;
import pe.joedayz.microservicios.order.client.CatalogWebClient;
import pe.joedayz.microservicios.order.client.InventoryGrpcClient;
import pe.joedayz.microservicios.order.client.ProductDto;
import pe.joedayz.microservicios.order.tenant.TenantContext;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CatalogRestClient restClient;
    private final CatalogWebClient webClient;
    private final CatalogFeignClient feignClient;
    private final InventoryGrpcClient inventoryClient;

    public CheckoutController(CatalogRestClient restClient,
                              CatalogWebClient webClient,
                              CatalogFeignClient feignClient,
                              InventoryGrpcClient inventoryClient) {
        this.restClient = restClient;
        this.webClient = webClient;
        this.feignClient = feignClient;
        this.inventoryClient = inventoryClient;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Client-Style", defaultValue = "restclient") String clientStyle) {

        String tenant = TenantContext.require();
        String style = clientStyle == null ? "restclient" : clientStyle.trim().toLowerCase();

        Optional<ProductDto> productOpt = switch (style) {
            case "webclient" -> webClient.findBySku(tenant, request.sku());
            case "feign" -> findWithFeign(request.sku());
            default -> restClient.findBySku(tenant, request.sku());
        };

        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductDto product = productOpt.get();
        CheckStockResponse stock = inventoryClient.checkStock(tenant, request.sku(), request.quantity());

        CheckoutResponse body = CheckoutResponse.of(
                style,
                product,
                request.quantity(),
                stock.getAvailable(),
                stock.getRemaining(),
                stock.getMessage()
        );

        if (!stock.getAvailable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        return ResponseEntity.ok(body);
    }

    private Optional<ProductDto> findWithFeign(String sku) {
        try {
            return Optional.ofNullable(feignClient.getBySku(sku));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        }
    }
}
