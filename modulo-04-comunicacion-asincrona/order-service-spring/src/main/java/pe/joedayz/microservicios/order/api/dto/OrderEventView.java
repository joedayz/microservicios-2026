package pe.joedayz.microservicios.order.api.dto;

import java.time.Instant;

import pe.joedayz.microservicios.order.eventstore.StoredEvent;

public record OrderEventView(
        String eventType,
        String payload,
        long version,
        Instant timestamp
) {
    public static OrderEventView from(StoredEvent event) {
        return new OrderEventView(event.getEventType(), event.getPayload(),
                event.getVersion(), event.getTimestamp());
    }
}
