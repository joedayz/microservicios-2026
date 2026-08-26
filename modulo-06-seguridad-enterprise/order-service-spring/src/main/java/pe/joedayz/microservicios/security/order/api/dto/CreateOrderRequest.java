package pe.joedayz.microservicios.security.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String sku,
        @Min(1) int quantity,
        @NotBlank String shippingRegion) {
}
