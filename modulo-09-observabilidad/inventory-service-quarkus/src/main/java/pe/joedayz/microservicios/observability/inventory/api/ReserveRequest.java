package pe.joedayz.microservicios.observability.inventory.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReserveRequest(
        @NotBlank String orderId,
        @NotBlank String sku,
        @Min(1) int qty) {
}
