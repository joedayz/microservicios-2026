namespace JoeDayz.Microservicios.Modulo01.Solid.Liskov;

/// <summary>LSP: cualquier implementacion debe poder sustituir a la interfaz sin sorpresas.</summary>
public interface IPaymentGateway
{
    void Pay(decimal amount);
}

public sealed class StripeGateway : IPaymentGateway
{
    public void Pay(decimal amount) => Console.WriteLine($"   [LSP] Pagando {amount} con Stripe");
}

public sealed class CulqiGateway : IPaymentGateway
{
    public void Pay(decimal amount) => Console.WriteLine($"   [LSP] Pagando {amount} con Culqi");
}

/// <summary>
/// ANTI-PATRON (viola LSP): implementa el contrato pero lanza excepcion.
/// El cliente no puede sustituirla por otra sin romperse.
/// </summary>
public sealed class BrokenGateway : IPaymentGateway
{
    public void Pay(decimal amount) => throw new NotSupportedException("Este gateway no soporta pagos");
}

public sealed class Checkout(IPaymentGateway gateway)
{
    public void Process(decimal amount) => gateway.Pay(amount);
}
