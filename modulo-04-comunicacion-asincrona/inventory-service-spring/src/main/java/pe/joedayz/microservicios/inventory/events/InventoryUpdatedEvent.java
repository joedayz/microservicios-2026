package pe.joedayz.microservicios.inventory.events;

/**
 * Publicado por Inventory Service tras decrementar el inventario físico.
 */
public record InventoryUpdatedEvent(
        String orderId,
        String tenantId,
        String sku,
        int remainingQuantity,
        long updatedAt
) {
}
