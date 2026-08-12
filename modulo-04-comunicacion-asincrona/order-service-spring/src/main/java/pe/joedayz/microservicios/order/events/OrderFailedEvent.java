package pe.joedayz.microservicios.order.events;

public record OrderFailedEvent(
        String orderId,
        String tenantId,
        String reason,
        long failedAt
) {
}
