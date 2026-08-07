using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Ddd.Shared;

/// <summary>
/// Evento de dominio: algo relevante que YA ocurrio en el negocio (pasado, inmutable).
/// Es el contrato que viaja por la outbox hacia Kafka / Azure Service Bus / SNS.
/// </summary>
public interface IDomainEvent
{
    TenantId TenantId { get; }

    DateTimeOffset OccurredOn { get; }

    /// <summary>Nombre logico del evento; miembro por defecto de la interfaz (C# 8+).</summary>
    string EventType => GetType().Name;
}
