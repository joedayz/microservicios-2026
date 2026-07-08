package pe.joedayz.microservicios.modulo01.ddd.order.events;

import pe.joedayz.microservicios.modulo01.ddd.order.OrderId;
import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.time.Instant;

/** Evento de dominio: el pedido quedo confirmado (pago cobrado, stock reservado, envio agendado). */
public record OrderConfirmed(
        TenantId tenantId,
        OrderId orderId,
        Instant occurredOn) implements DomainEvent {

    public static OrderConfirmed now(TenantId tenantId, OrderId orderId) {
        return new OrderConfirmed(tenantId, orderId, Instant.now());
    }
}
