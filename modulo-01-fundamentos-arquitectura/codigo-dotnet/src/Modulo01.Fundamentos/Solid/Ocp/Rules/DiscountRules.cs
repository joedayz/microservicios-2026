using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;

namespace JoeDayz.Microservicios.Modulo01.Solid.Ocp.Rules;

/// <summary>Descuento porcentual sobre el total del pedido.</summary>
public sealed class PercentageRule(decimal percentage) : IDiscountRule
{
    public string Name => $"Descuento del {percentage}%";

    public bool Applies(Order order) => true;

    public Money DiscountFor(Order order) =>
        new(order.Total().Amount * percentage / 100m, order.Total().Currency);
}

/// <summary>Envio gratis (se descuenta el costo de envio) si el pedido supera un umbral.</summary>
public sealed class FreeShippingOverAmountRule(Money threshold, Money shippingSaved) : IDiscountRule
{
    public string Name => $"Envio gratis por compra mayor a {threshold}";

    public bool Applies(Order order) => order.Total().IsGreaterThan(threshold);

    public Money DiscountFor(Order order) => shippingSaved;
}
