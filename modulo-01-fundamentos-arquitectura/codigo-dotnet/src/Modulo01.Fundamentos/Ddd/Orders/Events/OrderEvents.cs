using JoeDayz.Microservicios.Modulo01.Ddd.Shared;
using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders.Events;

public sealed record OrderPlaced(TenantId TenantId, OrderId OrderId, Money Total, DateTimeOffset OccurredOn)
    : IDomainEvent
{
    public static OrderPlaced Now(TenantId tenantId, OrderId orderId, Money total) =>
        new(tenantId, orderId, total, DateTimeOffset.UtcNow);
}

public sealed record OrderConfirmed(TenantId TenantId, OrderId OrderId, DateTimeOffset OccurredOn)
    : IDomainEvent
{
    public static OrderConfirmed Now(TenantId tenantId, OrderId orderId) =>
        new(tenantId, orderId, DateTimeOffset.UtcNow);
}

public sealed record OrderCancelled(TenantId TenantId, OrderId OrderId, string Reason, DateTimeOffset OccurredOn)
    : IDomainEvent
{
    public static OrderCancelled Now(TenantId tenantId, OrderId orderId, string reason) =>
        new(tenantId, orderId, reason, DateTimeOffset.UtcNow);
}
