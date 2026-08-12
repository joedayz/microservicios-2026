package pe.joedayz.microservicios.inventory.events;

/**
 * Consumido desde el topic "stock-reserved", publicado por Catalog Service (outbox).
 * Order Service (orquestador) también consume este mismo evento (choreography
 * híbrida: un evento, dos consumer groups).
 */
public record StockReservedEvent(
        String orderId,
        String tenantId,
        String sku,
        int quantity,
        long reservedAt
) {
}
