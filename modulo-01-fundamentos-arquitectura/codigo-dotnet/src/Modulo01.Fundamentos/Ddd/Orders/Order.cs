using JoeDayz.Microservicios.Modulo01.Ddd.Orders.Events;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;
using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders;

/// <summary>
/// RAIZ DEL AGREGADO. Toda modificacion pasa por aqui, por eso las invariantes
/// (moneda unica, monto maximo, transiciones de estado) no se pueden violar desde fuera.
/// Es tambien el limite de la transaccion: un pedido = una unidad de consistencia.
/// </summary>
public sealed class Order
{
    private static readonly Money MaxTotal = Money.Of(50_000.00m, "PEN");

    private readonly List<OrderLine> _lines = [];
    private readonly List<IDomainEvent> _domainEvents = [];

    private Order(OrderId id, TenantId tenantId, CustomerId customerId, string currency)
    {
        Id = id;
        TenantId = tenantId;
        CustomerId = customerId;
        Currency = currency;
        Status = OrderStatus.Draft;
    }

    public OrderId Id { get; }

    public TenantId TenantId { get; }

    public CustomerId CustomerId { get; }

    public string Currency { get; }

    public OrderStatus Status { get; private set; }

    public IReadOnlyList<OrderLine> Lines => _lines.AsReadOnly();

    /// <summary>Factory method: un pedido SIEMPRE nace valido y perteneciendo a un tenant.</summary>
    public static Order Create(TenantId tenantId, CustomerId customerId, string currency)
    {
        ArgumentNullException.ThrowIfNull(tenantId);
        ArgumentNullException.ThrowIfNull(customerId);
        return new Order(OrderId.New(), tenantId, customerId, currency);
    }

    public void AddLine(Sku sku, Quantity quantity, Money unitPrice)
    {
        if (Status is not OrderStatus.Draft)
        {
            throw new InvalidOperationException("Solo se pueden agregar lineas a un pedido en DRAFT");
        }

        if (unitPrice.Currency != Currency)
        {
            throw new ArgumentException("La linea usa una moneda distinta al pedido", nameof(unitPrice));
        }

        var line = new OrderLine(sku, quantity, unitPrice);
        if (Total().Plus(line.Subtotal()).IsGreaterThan(MaxTotal))
        {
            throw new InvalidOperationException($"El pedido supera el maximo permitido de {MaxTotal}");
        }

        _lines.Add(line);
    }

    public void Place()
    {
        if (Status is not OrderStatus.Draft)
        {
            throw new InvalidOperationException("Solo un pedido en DRAFT puede colocarse");
        }

        if (_lines.Count == 0)
        {
            throw new InvalidOperationException("No se puede colocar un pedido sin lineas");
        }

        Status = OrderStatus.Placed;
        _domainEvents.Add(OrderPlaced.Now(TenantId, Id, Total()));
    }

    public void Confirm()
    {
        if (Status is not OrderStatus.Placed)
        {
            throw new InvalidOperationException("Solo un pedido PLACED puede confirmarse");
        }

        Status = OrderStatus.Confirmed;
        _domainEvents.Add(OrderConfirmed.Now(TenantId, Id));
    }

    public void Cancel(string reason)
    {
        if (Status is OrderStatus.Confirmed)
        {
            throw new InvalidOperationException("Un pedido confirmado no se cancela: requiere flujo de devolucion");
        }

        Status = OrderStatus.Cancelled;
        _domainEvents.Add(OrderCancelled.Now(TenantId, Id, reason));
    }

    public Money Total() =>
        _lines.Aggregate(Money.Zero(Currency), (acc, line) => acc.Plus(line.Subtotal()));

    /// <summary>Entrega los eventos acumulados y limpia el buffer (los consume la capa de aplicacion).</summary>
    public IReadOnlyList<IDomainEvent> PullDomainEvents()
    {
        IReadOnlyList<IDomainEvent> copy = [.. _domainEvents];
        _domainEvents.Clear();
        return copy;
    }
}
