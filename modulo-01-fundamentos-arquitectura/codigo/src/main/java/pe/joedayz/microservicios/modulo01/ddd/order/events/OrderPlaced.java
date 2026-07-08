package pe.joedayz.microservicios.modulo01.ddd.order.events;

import pe.joedayz.microservicios.modulo01.ddd.order.OrderId;
import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.time.Instant;

/**
 * Evento de dominio: un pedido fue colocado (checkout). Dispara la Saga de compra.
 *
 * <p>Nota de versionado (LSP, doc 1): para evolucionar este contrato a v2, AGREGA
 * campos opcionales, no elimines ni cambies la semantica de los existentes.
 */
public record OrderPlaced(
        TenantId tenantId,
        OrderId orderId,
        Money total,
        Instant occurredOn) implements DomainEvent {

    public static OrderPlaced now(TenantId tenantId, OrderId orderId, Money total) {
        return new OrderPlaced(tenantId, orderId, total, Instant.now());
    }
}
