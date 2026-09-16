package pe.joedayz.microservicios.resilience.order.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
        @NotBlank String orderId,
        @NotBlank String sku,
        @Min(1) int qty) {
}

