package pe.joedayz.microservicios.catalog.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 60) String sku,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 300) String description,
        @NotBlank @Size(max = 80) String category,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
