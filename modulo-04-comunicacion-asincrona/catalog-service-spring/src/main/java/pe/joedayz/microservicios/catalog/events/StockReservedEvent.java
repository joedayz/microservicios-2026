package pe.joedayz.microservicios.catalog.events;

/**
 * Publicado por Catalog Service (vía outbox) tras reservar stock exitosamente.
 * Sirve como reply al orquestador (Order Service) y como broadcast para Inventory Service.
 */
public record StockReservedEvent(
        String orderId,
        String tenantId,
        String sku,
        int quantity,
        long reservedAt
) {
}
