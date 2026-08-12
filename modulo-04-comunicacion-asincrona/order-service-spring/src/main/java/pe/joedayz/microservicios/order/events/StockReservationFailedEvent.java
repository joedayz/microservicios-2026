package pe.joedayz.microservicios.order.events;

/**
 * Consumido desde el topic "stock-reservation-failed", publicado por Catalog Service.
 */
public record StockReservationFailedEvent(
        String orderId,
        String tenantId,
        String sku,
        int requestedQuantity,
        String reason
) {
}
