package pe.joedayz.microservicios.catalog.api;

import java.util.NoSuchElementException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.catalog.api.dto.StockResponse;
import pe.joedayz.microservicios.catalog.repository.ProductStockRepository;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final ProductStockRepository productStockRepository;

    public StockController(ProductStockRepository productStockRepository) {
        this.productStockRepository = productStockRepository;
    }

    @GetMapping("/{sku}")
    public StockResponse getStock(@PathVariable String sku, @RequestHeader("X-Tenant-ID") String tenantId) {
        return productStockRepository.findBySkuAndTenantId(sku, tenantId)
                .map(StockResponse::from)
                .orElseThrow(() -> new NoSuchElementException("SKU no encontrado: " + sku));
    }
}
