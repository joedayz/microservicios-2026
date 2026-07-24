package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.repository.ProductRepository;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import pe.joedayz.microservicios.catalog.tenant.TenantWebFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/products")
@Tag(name = "Products v2", description = "Catalogo — API version 2 (URI). Respuesta enriquecida.")
public class ProductV2Controller {

    private final ProductRepository repository;

    public ProductV2Controller(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Listar productos (v2)")
    @Parameter(name = TenantWebFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true,
            example = "tienda-deportes")
    public List<ProductV2Response> list() {
        return repository.findByTenantId(TenantContext.require()).stream()
                .map(ProductV2Response::from)
                .toList();
    }
}
