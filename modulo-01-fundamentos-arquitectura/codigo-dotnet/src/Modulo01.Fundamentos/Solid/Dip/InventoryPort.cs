using System.Collections.Concurrent;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;
using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Solid.Dip;

/// <summary>
/// DIP: el caso de uso depende de esta ABSTRACCION, no del adaptador concreto.
/// Manana el inventario puede ser gRPC, REST o una cola: el dominio no cambia.
/// </summary>
public interface IInventoryPort
{
    bool Reserve(TenantId tenantId, Sku sku, Quantity quantity);

    void Release(TenantId tenantId, Sku sku, Quantity quantity);
}

/// <summary>Adaptador en memoria del puerto de inventario (aislado por tenant).</summary>
public sealed class InMemoryInventoryAdapter : IInventoryPort
{
    private readonly ConcurrentDictionary<string, int> _stock = new();

    public void SetStock(TenantId tenantId, Sku sku, int units) => _stock[Key(tenantId, sku)] = units;

    public int Available(TenantId tenantId, Sku sku) => _stock.GetValueOrDefault(Key(tenantId, sku), 0);

    public bool Reserve(TenantId tenantId, Sku sku, Quantity quantity)
    {
        var key = Key(tenantId, sku);
        var current = _stock.GetValueOrDefault(key, 0);
        if (current < quantity.Value)
        {
            return false;
        }

        _stock[key] = current - quantity.Value;
        return true;
    }

    public void Release(TenantId tenantId, Sku sku, Quantity quantity) =>
        _stock.AddOrUpdate(Key(tenantId, sku), quantity.Value, (_, current) => current + quantity.Value);

    private static string Key(TenantId tenantId, Sku sku) => $"{tenantId.Value}::{sku.Value}";
}
