using System.Collections.Concurrent;
using JoeDayz.Microservicios.Modulo01.Ddd.Orders.Events;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;

namespace JoeDayz.Microservicios.Modulo01.Patterns.Cqrs;

/// <summary>Modelo de LECTURA: denormalizado y listo para pintar en pantalla, sin joins.</summary>
public sealed record OrderSummaryReadModel(string TenantId, string OrderId, string Total, string Status);

/// <summary>Almacen de lectura (en produccion: Redis, Cosmos DB, Elasticsearch o una vista materializada).</summary>
public sealed class OrderReadStore
{
    private readonly ConcurrentDictionary<string, OrderSummaryReadModel> _summaries = new();

    public void Put(OrderSummaryReadModel summary) =>
        _summaries[Key(summary.TenantId, summary.OrderId)] = summary;

    public OrderSummaryReadModel? Get(string tenantId, string orderId) =>
        _summaries.GetValueOrDefault(Key(tenantId, orderId));

    private static string Key(string tenantId, string orderId) => $"{tenantId}::{orderId}";
}

/// <summary>
/// Proyector: consume eventos del broker y mantiene el read model.
/// El <c>switch</c> con pattern matching sobre tipos es el equivalente C# del
/// switch por patrones de Java 21.
/// </summary>
public sealed class OrderProjector(OrderReadStore readStore)
{
    public void On(IDomainEvent domainEvent)
    {
        switch (domainEvent)
        {
            case OrderPlaced e:
                readStore.Put(new OrderSummaryReadModel(
                    e.TenantId.Value, e.OrderId.ToString(), e.Total.ToString(), "PLACED"));
                break;
            case OrderConfirmed e:
                UpdateStatus(e.TenantId.Value, e.OrderId.ToString(), "CONFIRMED");
                break;
            case OrderCancelled e:
                UpdateStatus(e.TenantId.Value, e.OrderId.ToString(), "CANCELLED");
                break;
            default:
                break; // evento no relevante para esta proyeccion
        }
    }

    private void UpdateStatus(string tenantId, string orderId, string status)
    {
        if (readStore.Get(tenantId, orderId) is { } current)
        {
            readStore.Put(current with { Status = status });
        }
    }
}

/// <summary>Lado Query: solo lee del read store, nunca toca el agregado.</summary>
public sealed class OrderQueries(OrderReadStore readStore)
{
    public OrderSummaryReadModel? FindSummary(string tenantId, string orderId) =>
        readStore.Get(tenantId, orderId);
}
