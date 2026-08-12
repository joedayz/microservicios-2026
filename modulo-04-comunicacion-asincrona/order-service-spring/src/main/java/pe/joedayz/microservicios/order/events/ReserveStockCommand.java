package pe.joedayz.microservicios.order.events;

/**
 * Comando enviado por el orquestador a Catalog Service (topic: reserve-stock-command).
 */
public record ReserveStockCommand(
        String orderId,
        String tenantId,
        String sku,
        int quantity
) {
}
