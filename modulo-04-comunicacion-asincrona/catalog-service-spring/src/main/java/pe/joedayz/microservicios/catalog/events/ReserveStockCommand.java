package pe.joedayz.microservicios.catalog.events;

/**
 * Consumido desde el topic "reserve-stock-command", publicado por Order Service (orquestador).
 */
public record ReserveStockCommand(
        String orderId,
        String tenantId,
        String sku,
        int quantity
) {
}
