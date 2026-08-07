using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;

namespace JoeDayz.Microservicios.Modulo01.Solid.Ocp;

/// <summary>
/// OCP: una politica de descuento. Para agregar una nueva se crea otra implementacion;
/// el <see cref="DiscountEngine"/> NO se toca (cerrado a modificacion, abierto a extension).
/// </summary>
public interface IDiscountRule
{
    string Name { get; }

    bool Applies(Order order);

    Money DiscountFor(Order order);
}

/// <summary>ANTI-PATRON (viola OCP): cada nuevo tipo de cliente obliga a editar este metodo.</summary>
public sealed class CalcularDescuento
{
    public decimal Calcular(decimal total, string tipoCliente) => tipoCliente switch
    {
        "ESTUDIANTE" => total * 0.10m,
        "JUBILADO" => total * 0.15m,
        "VIP" => total * 0.20m,
        _ => 0m
    };
}

/// <summary>Motor de descuentos: recorre las reglas configuradas y suma las que aplican.</summary>
public sealed class DiscountEngine(IEnumerable<IDiscountRule> rules)
{
    private readonly IReadOnlyList<IDiscountRule> _rules = [.. rules];

    public Money TotalDiscount(Order order) =>
        _rules.Where(rule => rule.Applies(order))
              .Aggregate(Money.Zero(order.Total().Currency), (acc, rule) => acc.Plus(rule.DiscountFor(order)));
}
