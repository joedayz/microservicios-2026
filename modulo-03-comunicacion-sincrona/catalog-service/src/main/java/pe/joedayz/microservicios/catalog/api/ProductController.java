package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.repository.ProductRepository;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import pe.joedayz.microservicios.catalog.tenant.TenantWebFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products v1", description = "Catalogo de productos — API version 1 (URI)")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Listar productos del tenant")
    @Parameter(name = TenantWebFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true,
            description = "Identificador del tenant", example = "tienda-deportes")
    public List<ProductResponse> list() {
        return repository.findByTenantId(TenantContext.require()).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Obtener producto por SKU")
    @Parameter(name = TenantWebFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true,
            example = "tienda-deportes")
    public ResponseEntity<ProductResponse> get(@PathVariable String sku) {
        return repository.findByTenantIdAndSku(TenantContext.require(), sku)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
