using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;
using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Patterns.Saga;

/// <summary>Puerto de pagos: el dominio no sabe si detras hay Stripe, Culqi o Niubiz.</summary>
public interface IPaymentPort
{
    string Charge(TenantId tenantId, OrderId orderId, Money amount);

    void Refund(TenantId tenantId, string transactionId);
}

/// <summary>Adaptador de pagos simulado.</summary>
public sealed class InMemoryPaymentAdapter : IPaymentPort
{
    public string Charge(TenantId tenantId, OrderId orderId, Money amount)
    {
        var txId = $"tx-{Guid.NewGuid().ToString()[..8]}";
        Console.WriteLine($"   [Payment] cobrado {amount} por pedido {orderId} -> {txId}");
        return txId;
    }

    public void Refund(TenantId tenantId, string transactionId) =>
        Console.WriteLine($"   [Payment] COMPENSACION: reembolsado {transactionId}");
}

/// <summary>Falla de un paso de la saga: dispara las compensaciones.</summary>
public sealed class SagaStepFailedException(string message) : Exception(message);

public sealed record SagaResult(bool Success, string Detail)
{
    public static SagaResult Confirmed() => new(true, "Pedido confirmado");

    public static SagaResult Cancelled(string reason) => new(false, $"Pedido cancelado: {reason}");
}
