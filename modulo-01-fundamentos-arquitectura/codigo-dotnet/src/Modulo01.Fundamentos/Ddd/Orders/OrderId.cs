namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders;

/// <summary>Identidad del agregado Pedido.</summary>
public readonly record struct OrderId(Guid Value)
{
    public static OrderId New() => new(Guid.NewGuid());

    public static OrderId Of(string value) => new(Guid.Parse(value));

    public override string ToString() => Value.ToString();
}
