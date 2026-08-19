package pe.joedayz.microservicios.inventory.api;

import pe.joedayz.microservicios.inventory.domain.InventoryItem;

public record InventoryResponse(
        String sku,
        String name,
        String warehouseCode,
        int availableQuantity,
        int reservedQuantity
) {

    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(
                item.sku,
                item.name,
                item.warehouseCode,
                item.availableQuantity,
                item.reservedQuantity);
    }
}
