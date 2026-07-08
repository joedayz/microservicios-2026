package pe.joedayz.microservicios.modulo01.ddd.order;

import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.util.Optional;

/**
 * Puerto (DIP) para persistir el agregado {@link Order}.
 *
 * <p>La interfaz vive en el DOMINIO; su implementacion (JPA, jOOQ, Mongo...) es un
 * ADAPTADOR en la capa de infraestructura. El dominio no sabe como se guarda.
 *
 * <p>Nota multi-tenant: toda operacion recibe el {@link TenantId} para garantizar
 * que jamas se lea/escriba el pedido de otro tenant.
 */
public interface OrderRepository {

    Optional<Order> findById(TenantId tenantId, OrderId id);

    void save(Order order);
}
