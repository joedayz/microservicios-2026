using JoeDayz.Microservicios.Modulo01.Ddd.Shared;

namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders;

/// <summary>
/// Entidad interna del agregado: nunca se modifica desde fuera, solo a traves de <see cref="Order"/>.
/// </summary>
public sealed record OrderLine(Sku Sku, Quantity Quantity, Money UnitPrice)
{
    public Money Subtotal() => UnitPrice.Times(Quantity.Value);
}
