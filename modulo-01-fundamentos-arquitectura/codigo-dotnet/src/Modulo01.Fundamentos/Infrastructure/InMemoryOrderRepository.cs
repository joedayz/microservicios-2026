using System.Collections.Concurrent;
using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Infrastructure;

/// <summary>
/// Adaptador de persistencia en memoria. La clave incluye el tenant: es la forma mas
/// simple de aislamiento multi-tenant (equivalente a un filtro global en EF Core).
/// </summary>
public sealed class InMemoryOrderRepository : IOrderRepository
{
    private readonly ConcurrentDictionary<string, Order> _store = new();

    public Order? FindById(TenantId tenantId, OrderId id) =>
        _store.GetValueOrDefault(Key(tenantId, id));

    public void Save(Order order) => _store[Key(order.TenantId, order.Id)] = order;

    private static string Key(TenantId tenantId, OrderId id) => $"{tenantId.Value}::{id.Value}";
}
