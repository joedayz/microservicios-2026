package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.repository.ProductRepository;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return repository.findByTenantId(TenantContext.require()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> get(@PathVariable String sku) {
        return repository.findByTenantIdAndSku(TenantContext.require(), sku)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
