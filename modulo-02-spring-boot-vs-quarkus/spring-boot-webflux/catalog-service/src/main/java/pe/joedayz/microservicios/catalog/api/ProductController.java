package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.repository.ProductR2dbcRepository;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductR2dbcRepository repository;

    public ProductController(ProductR2dbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Flux<ProductResponse> list() {
        return TenantContext.require()
                .flatMapMany(repository::findByTenantId)
                .map(ProductResponse::from);
    }

    @GetMapping("/{sku}")
    public Mono<ProductResponse> get(@PathVariable String sku) {
        return TenantContext.require()
                .flatMap(tenant -> repository.findByTenantIdAndSku(tenant, sku))
                .map(ProductResponse::from);
    }
}
