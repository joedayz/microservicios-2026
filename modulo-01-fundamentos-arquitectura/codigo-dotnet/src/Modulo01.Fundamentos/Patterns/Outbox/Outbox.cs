using JoeDayz.Microservicios.Modulo01.Ddd.Shared;

namespace JoeDayz.Microservicios.Modulo01.Patterns.Outbox;

/// <summary>Fila de la tabla outbox: el evento y su marca de publicado.</summary>
public sealed class OutboxMessage(IDomainEvent domainEvent)
{
    public IDomainEvent Event { get; } = domainEvent;

    public bool Published { get; private set; }

    public void MarkPublished() => Published = true;
}

/// <summary>Tabla outbox: vive en la MISMA base de datos que el agregado, por eso el commit es atomico.</summary>
public sealed class OutboxStore
{
    private readonly List<OutboxMessage> _messages = [];

    public void Add(IDomainEvent domainEvent) => _messages.Add(new OutboxMessage(domainEvent));

    public IReadOnlyList<OutboxMessage> Pending() => [.. _messages.Where(m => !m.Published)];
}

/// <summary>
/// Relay / message dispatcher: lee lo pendiente y lo publica al broker.
/// En produccion es un worker aparte (o CDC con Debezium) con reintentos y at-least-once.
/// </summary>
public sealed class OutboxRelay(OutboxStore outbox, Messaging.EventBus eventBus)
{
    public int RelayPending()
    {
        var count = 0;
        foreach (var message in outbox.Pending())
        {
            eventBus.Publish(message.Event);
            message.MarkPublished();
            count++;
        }

        return count;
    }
}
