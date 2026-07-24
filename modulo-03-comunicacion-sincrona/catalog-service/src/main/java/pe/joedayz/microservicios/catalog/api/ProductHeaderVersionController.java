package pe.joedayz.microservicios.catalog.api;

import pe.joedayz.microservicios.catalog.repository.ProductRepository;
import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import pe.joedayz.microservicios.catalog.tenant.TenantWebFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Demo de versionado por header {@code API-Version} (sin cambiar la URI).
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products header-versioned", description = "Misma URI; version via header API-Version")
public class ProductHeaderVersionController {

    private final ProductRepository repository;

    public ProductHeaderVersionController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Listar productos segun API-Version")
    @Parameter(name = TenantWebFilter.TENANT_HEADER, in = ParameterIn.HEADER, required = true,
            example = "tienda-deportes")
    @Parameter(name = "API-Version", in = ParameterIn.HEADER, required = false,
            description = "1 (default) o 2", example = "2")
    public Object list(@RequestHeader(value = "API-Version", defaultValue = "1") String apiVersion) {
        String tenant = TenantContext.require();
        if ("2".equals(apiVersion.trim())) {
            return repository.findByTenantId(tenant).stream()
                    .map(ProductV2Response::from)
                    .toList();
        }
        List<ProductResponse> v1 = repository.findByTenantId(tenant).stream()
                .map(ProductResponse::from)
                .toList();
        return Map.of("apiVersion", "1", "items", v1);
    }
}
