package pe.joedayz.microservicios.modulo01.ddd.order.events;

import pe.joedayz.microservicios.modulo01.ddd.order.OrderId;
import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.time.Instant;

/**
 * Evento de dominio: el pedido fue cancelado. Lo emite la Saga cuando un paso falla
 * y se ejecutan las compensaciones (ej. no habia stock, se reembolso el pago).
 */
public record OrderCancelled(
        TenantId tenantId,
        OrderId orderId,
        String reason,
        Instant occurredOn) implements DomainEvent {

    public static OrderCancelled now(TenantId tenantId, OrderId orderId, String reason) {
        return new OrderCancelled(tenantId, orderId, reason, Instant.now());
    }
}
