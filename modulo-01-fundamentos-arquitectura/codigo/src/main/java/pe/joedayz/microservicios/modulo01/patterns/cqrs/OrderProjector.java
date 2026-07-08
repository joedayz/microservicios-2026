package pe.joedayz.microservicios.modulo01.patterns.cqrs;

import pe.joedayz.microservicios.modulo01.ddd.order.events.OrderCancelled;
import pe.joedayz.microservicios.modulo01.ddd.order.events.OrderConfirmed;
import pe.joedayz.microservicios.modulo01.ddd.order.events.OrderPlaced;
import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;

/**
 * PROYECTOR (CQRS): consume los eventos de dominio y mantiene actualizado el read model.
 *
 * <p>Es el puente entre el lado de escritura (agregado) y el de lectura (vistas). Se
 * suscribe al bus de eventos (Kafka) y por cada evento actualiza {@link OrderReadStore}.
 * Esto introduce consistencia EVENTUAL entre escritura y lectura (milisegundos de retraso).
 */
public class OrderProjector {

    private final OrderReadStore readStore;

    public OrderProjector(OrderReadStore readStore) {
        this.readStore = readStore;
    }

    public void on(DomainEvent event) {
        switch (event) {
            case OrderPlaced e -> readStore.put(new OrderSummaryReadModel(
                    e.tenantId().value(), e.orderId().toString(), e.total().toString(), "PLACED"));
            case OrderConfirmed e -> updateStatus(e.tenantId().value(), e.orderId().toString(), "CONFIRMED");
            case OrderCancelled e -> updateStatus(e.tenantId().value(), e.orderId().toString(), "CANCELLED");
            default -> { /* evento no relevante para esta proyeccion */ }
        }
    }

    private void updateStatus(String tenantId, String orderId, String status) {
        OrderSummaryReadModel current = readStore.get(tenantId, orderId);
        if (current != null) {
            readStore.put(new OrderSummaryReadModel(tenantId, orderId, current.total(), status));
        }
    }
}
