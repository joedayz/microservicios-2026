using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders;

/// <summary>
/// Puerto de persistencia del agregado (DIP: el dominio define la interfaz,
/// la infraestructura la implementa). Siempre se consulta con TenantId.
/// </summary>
public interface IOrderRepository
{
    Order? FindById(TenantId tenantId, OrderId id);

    void Save(Order order);
}
