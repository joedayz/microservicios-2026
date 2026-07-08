package pe.joedayz.microservicios.modulo01.ddd.order;

import pe.joedayz.microservicios.modulo01.ddd.order.events.OrderCancelled;
import pe.joedayz.microservicios.modulo01.ddd.order.events.OrderConfirmed;
import pe.joedayz.microservicios.modulo01.ddd.order.events.OrderPlaced;
import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.ddd.shared.Quantity;
import pe.joedayz.microservicios.modulo01.ddd.shared.Sku;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AGGREGATE ROOT del contexto de Pedidos.
 *
 * <p>Es la unica puerta de entrada al agregado: el mundo exterior NO manipula
 * {@link OrderLine} directamente, sino a traves de {@code addLine(...)}. Asi la raiz
 * garantiza los INVARIANTES del negocio en todo momento:
 * <ul>
 *   <li>Todo pedido pertenece a un {@link TenantId} (multi-tenancy).</li>
 *   <li>Solo se pueden agregar lineas mientras esta en DRAFT.</li>
 *   <li>No se puede colocar (place) un pedido sin lineas.</li>
 *   <li>El total no puede superar el limite de la plataforma.</li>
 *   <li>Las transiciones de estado son controladas (DRAFT -> PLACED -> CONFIRMED/CANCELLED).</li>
 * </ul>
 *
 * <p>Acumula {@link DomainEvent}s que luego se publican (ver patron Outbox). Esto es
 * consistencia transaccional DENTRO del agregado; entre agregados/servicios se usa
 * consistencia eventual (Saga).
 */
public class Order {

    private static final Money MAX_TOTAL = Money.of("50000.00", "PEN");

    private final OrderId id;
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final String currency;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Order(OrderId id, TenantId tenantId, CustomerId customerId, String currency) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.currency = currency;
        this.status = OrderStatus.DRAFT;
    }

    /** Factory: crea un pedido nuevo en estado DRAFT. Exige tenant (nunca hay pedido sin tenant). */
    public static Order create(TenantId tenantId, CustomerId customerId, String currency) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Un pedido siempre pertenece a un tenant");
        }
        return new Order(OrderId.newId(), tenantId, customerId, currency);
    }

    public void addLine(Sku sku, Quantity quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden agregar lineas a un pedido en DRAFT");
        }
        if (!unitPrice.currency().equals(currency)) {
            throw new IllegalArgumentException("La linea usa una moneda distinta al pedido");
        }
        OrderLine line = new OrderLine(sku, quantity, unitPrice);
        Money nuevoTotal = total().plus(line.subtotal());
        if (nuevoTotal.isGreaterThan(MAX_TOTAL)) {
            throw new IllegalStateException("El pedido supera el maximo permitido de " + MAX_TOTAL);
        }
        lines.add(line);
    }

    /** Coloca el pedido (checkout). Emite {@link OrderPlaced}. */
    public void place() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Solo un pedido en DRAFT puede colocarse");
        }
        if (lines.isEmpty()) {
            throw new IllegalStateException("No se puede colocar un pedido sin lineas");
        }
        status = OrderStatus.PLACED;
        registerEvent(OrderPlaced.now(tenantId, id, total()));
    }

    /** Confirma el pedido tras completarse la Saga (pago + stock + envio). */
    public void confirm() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Solo un pedido PLACED puede confirmarse");
        }
        status = OrderStatus.CONFIRMED;
        registerEvent(OrderConfirmed.now(tenantId, id));
    }

    /** Cancela el pedido (compensacion de la Saga). */
    public void cancel(String reason) {
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Un pedido confirmado no se cancela: requiere flujo de devolucion");
        }
        status = OrderStatus.CANCELLED;
        registerEvent(OrderCancelled.now(tenantId, id, reason));
    }

    public Money total() {
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.zero(currency), Money::plus);
    }

    private void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Devuelve los eventos acumulados y los limpia (los toma el Outbox para publicarlos). */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public OrderId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderLine> lines() {
        return Collections.unmodifiableList(lines);
    }
}
