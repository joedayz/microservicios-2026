package pe.joedayz.microservicios.catalog.events;

/**
 * Publicado por Catalog Service (vía outbox) cuando no hay stock suficiente.
 */
public record StockReservationFailedEvent(
        String orderId,
        String tenantId,
        String sku,
        int requestedQuantity,
        String reason
) {
}
