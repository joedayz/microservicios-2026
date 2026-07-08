package pe.joedayz.microservicios.modulo01.app;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.order.OrderRepository;
import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;
import pe.joedayz.microservicios.modulo01.patterns.outbox.OutboxStore;

/**
 * Servicio de aplicacion (caso de uso). Orquesta el agregado y la persistencia, pero
 * NO contiene reglas de negocio (esas viven en el agregado {@link Order}).
 *
 * <p>Aqui se ve el patron TRANSACTIONAL OUTBOX: en una unica "transaccion" guardamos el
 * pedido Y sus eventos de dominio en la outbox. Al ser atomico, es imposible tener un
 * pedido guardado sin su evento (evita el problema del dual-write).
 */
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OutboxStore outbox;

    public OrderApplicationService(OrderRepository orderRepository, OutboxStore outbox) {
        this.orderRepository = orderRepository;
        this.outbox = outbox;
    }

    /**
     * Persiste el estado del pedido y vuelca sus eventos de dominio a la outbox,
     * todo en la misma unidad de trabajo (simulada).
     */
    public void saveWithEvents(Order order) {
        // --- inicio transaccion (BEGIN) ---
        orderRepository.save(order);
        for (DomainEvent event : order.pullDomainEvents()) {
            outbox.add(event);
        }
        // --- fin transaccion (COMMIT) : pedido + eventos, atomico ---
    }
}
