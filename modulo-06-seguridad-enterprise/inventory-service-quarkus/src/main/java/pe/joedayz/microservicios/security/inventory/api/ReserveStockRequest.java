package pe.joedayz.microservicios.security.inventory.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReserveStockRequest(
        @Min(1) int quantity,
        @NotBlank String region) {
}
