package pe.joedayz.microservicios.inventory.api;

import jakarta.validation.constraints.Min;

public record ReserveStockRequest(@Min(1) int quantity) {
}
