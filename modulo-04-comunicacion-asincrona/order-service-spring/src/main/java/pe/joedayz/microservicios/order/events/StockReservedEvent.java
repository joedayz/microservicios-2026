package pe.joedayz.microservicios.order.events;

/**
 * Consumido desde el topic "stock-reserved", publicado por Catalog Service (outbox).
 * Sirve como reply al orquestador (mismo evento que consume Inventory Service).
 */
public record StockReservedEvent(
        String orderId,
        String tenantId,
        String sku,
        int quantity,
        long reservedAt
) {
}
