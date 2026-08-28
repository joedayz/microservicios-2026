package pe.joedayz.microservicios.security.inventory.api;

public record InventoryItemResponse(
        String tenantId,
        String sku,
        int availableQuantity,
        int reservedQuantity,
        String warehouseRegion) {
}
