package pe.joedayz.microservicios.modulo01.infra;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.order.OrderId;
import pe.joedayz.microservicios.modulo01.ddd.order.OrderRepository;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADAPTADOR en memoria del puerto {@link OrderRepository} (DIP en accion).
 *
 * <p>La clave incluye el tenant, de modo que un {@code findById} nunca puede devolver
 * el pedido de otro tenant aunque coincidiera el OrderId.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> findById(TenantId tenantId, OrderId id) {
        return Optional.ofNullable(store.get(key(tenantId, id)));
    }

    @Override
    public void save(Order order) {
        store.put(key(order.tenantId(), order.id()), order);
    }

    private String key(TenantId tenantId, OrderId id) {
        return tenantId.value() + "::" + id.value();
    }
}
