using JoeDayz.Microservicios.Modulo01.Ddd.Shared;

namespace JoeDayz.Microservicios.Modulo01.Messaging;

/// <summary>
/// Broker simulado (en produccion: Kafka, Azure Service Bus o Amazon SNS/SQS).
/// Lo importante didacticamente es el desacople: quien publica no conoce a quien consume.
/// </summary>
public sealed class EventBus
{
    private readonly List<Action<IDomainEvent>> _subscribers = [];

    public void Subscribe(Action<IDomainEvent> subscriber) => _subscribers.Add(subscriber);

    public void Publish(IDomainEvent domainEvent)
    {
        Console.WriteLine($"   [Kafka] publicando {domainEvent.EventType} (tenant={domainEvent.TenantId})");
        foreach (var subscriber in _subscribers)
        {
            subscriber(domainEvent);
        }
    }
}
