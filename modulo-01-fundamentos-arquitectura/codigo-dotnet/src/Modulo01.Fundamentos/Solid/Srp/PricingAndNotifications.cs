using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;
using JoeDayz.Microservicios.Modulo01.Solid.Ocp;

namespace JoeDayz.Microservicios.Modulo01.Solid.Srp;

/// <summary>
/// ANTI-PATRON (viola SRP): una clase que calcula, persiste y notifica.
/// Tiene tres razones para cambiar -> tres equipos pisandose el mismo archivo.
/// </summary>
public sealed class OrderManager
{
    public void ProcessOrder(decimal amount)
    {
        var total = amount * 1.18m;
        Console.WriteLine($"   [SRP-malo] Total: {total}");
        Console.WriteLine("   [SRP-malo] Guardando pedido...");
        Console.WriteLine("   [SRP-malo] Enviando correo al cliente...");
    }
}

/// <summary>SRP: solo calcula precios (usa el motor de descuentos, no lo implementa).</summary>
public sealed class OrderPricingService(DiscountEngine discountEngine)
{
    public Money FinalPrice(Order order)
    {
        var total = order.Total();
        var result = total.Minus(discountEngine.TotalDiscount(order));
        return result.IsNegative ? Money.Zero(total.Currency) : result;
    }
}

/// <summary>SRP: solo notifica. En produccion seria un consumidor del evento OrderConfirmed.</summary>
public sealed class OrderNotificationService
{
    public void NotifyConfirmed(Order order) =>
        Console.WriteLine($"   [Notification] Email al cliente {order.CustomerId}: tu pedido {order.Id} fue confirmado.");
}
