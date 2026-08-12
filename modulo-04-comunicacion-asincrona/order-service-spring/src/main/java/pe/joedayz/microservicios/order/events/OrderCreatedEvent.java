package pe.joedayz.microservicios.order.events;

/**
 * Publicado por Order Service (orquestador) al iniciar la saga de checkout.
 */
public record OrderCreatedEvent(
        String orderId,
        String tenantId,
        String customerId,
        String sku,
        int quantity,
        long createdAt
) {
}
