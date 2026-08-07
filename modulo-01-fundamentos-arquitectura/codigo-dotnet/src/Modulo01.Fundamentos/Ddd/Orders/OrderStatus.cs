namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders;

/// <summary>Ciclo de vida del pedido. Las transiciones validas las controla el agregado.</summary>
public enum OrderStatus
{
    Draft,
    Placed,
    Confirmed,
    Cancelled
}
