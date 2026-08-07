using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Patterns.Outbox;

namespace JoeDayz.Microservicios.Modulo01.Application;

/// <summary>
/// Servicio de aplicacion (caso de uso). Orquesta agregado + persistencia, pero NO
/// contiene reglas de negocio (esas viven en <see cref="Order"/>).
/// Aqui se ve el TRANSACTIONAL OUTBOX: pedido y eventos se guardan en la MISMA
/// transaccion, de modo que es imposible tener un pedido sin su evento (dual-write).
/// </summary>
public sealed class OrderApplicationService(IOrderRepository orderRepository, OutboxStore outbox)
{
    public void SaveWithEvents(Order order)
    {
        // --- BEGIN TRANSACTION ---
        orderRepository.Save(order);
        foreach (var domainEvent in order.PullDomainEvents())
        {
            outbox.Add(domainEvent);
        }
        // --- COMMIT: pedido + eventos, atomico ---
    }
}
