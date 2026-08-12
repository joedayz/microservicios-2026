package pe.joedayz.microservicios.inventory.api.dto;

import pe.joedayz.microservicios.inventory.domain.InventoryItem;

public record InventoryResponse(String sku, String tenantId, int physicalQuantity) {

    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(item.getSku(), item.getTenantId(), item.getPhysicalQuantity());
    }
}
