package pe.joedayz.microservicios.order.events;

public record OrderConfirmedEvent(
        String orderId,
        String tenantId,
        long confirmedAt
) {
}
