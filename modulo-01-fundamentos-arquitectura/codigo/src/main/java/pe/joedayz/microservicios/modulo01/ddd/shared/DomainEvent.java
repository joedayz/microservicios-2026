package pe.joedayz.microservicios.modulo01.ddd.shared;

import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.time.Instant;

/**
 * Contrato de un Evento de Dominio: algo relevante que YA ocurrio en el negocio
 * (siempre en pasado: OrderPlaced, PaymentConfirmed...).
 *
 * <p>Todo evento lleva el {@link TenantId} para que la propagacion multi-tenant se
 * mantenga tambien en la comunicacion asincrona (Kafka).
 */
public interface DomainEvent {

    TenantId tenantId();

    Instant occurredOn();

    /** Nombre estable del evento; util para el versionado de contratos (LSP). */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
