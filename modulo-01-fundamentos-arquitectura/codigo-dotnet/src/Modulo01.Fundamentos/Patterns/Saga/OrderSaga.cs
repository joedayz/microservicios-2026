using JoeDayz.Microservicios.Modulo01.Application;
using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Solid.Dip;

namespace JoeDayz.Microservicios.Modulo01.Patterns.Saga;

/// <summary>
/// SAGA ORQUESTADA. Sin transacciones distribuidas (2PC): cada paso se confirma solo y,
/// si algo falla, se ejecutan las compensaciones en orden inverso (pila LIFO).
/// Pasos: cobrar pago -> reservar stock -> confirmar pedido.
/// </summary>
public sealed class OrderSaga(
    IPaymentPort paymentPort,
    IInventoryPort inventoryPort,
    OrderApplicationService orderApplicationService)
{
    public SagaResult Execute(Order order)
    {
        var compensations = new Stack<Action>();
        try
        {
            // Paso 1: cobrar el pago
            var txId = paymentPort.Charge(order.TenantId, order.Id, order.Total());
            compensations.Push(() => paymentPort.Refund(order.TenantId, txId));

            // Paso 2: reservar stock de cada linea
            foreach (var line in order.Lines)
            {
                if (!inventoryPort.Reserve(order.TenantId, line.Sku, line.Quantity))
                {
                    throw new SagaStepFailedException($"Sin stock para {line.Sku} x{line.Quantity}");
                }

                Console.WriteLine($"   [Inventory] reservadas {line.Quantity} uds de {line.Sku}");
                compensations.Push(() => inventoryPort.Release(order.TenantId, line.Sku, line.Quantity));
            }

            // Paso 3: confirmar el pedido (emite OrderConfirmed)
            order.Confirm();
            orderApplicationService.SaveWithEvents(order);
            return SagaResult.Confirmed();
        }
        catch (SagaStepFailedException failure)
        {
            Console.WriteLine($"   [Saga] FALLO: {failure.Message} -> compensando...");
            while (compensations.Count > 0)
            {
                compensations.Pop()();
            }

            order.Cancel(failure.Message);
            orderApplicationService.SaveWithEvents(order);
            return SagaResult.Cancelled(failure.Message);
        }
    }
}
