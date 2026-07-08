package pe.joedayz.microservicios.modulo01.patterns.outbox;

import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;

/**
 * Una fila de la tabla {@code outbox}. Guarda el evento pendiente de publicar y si ya
 * fue enviado. En el mundo real serialazariamos el evento a JSON/Avro; aqui guardamos
 * el objeto directo por simplicidad.
 */
public class OutboxMessage {

    private final DomainEvent event;
    private boolean published;

    public OutboxMessage(DomainEvent event) {
        this.event = event;
        this.published = false;
    }

    public DomainEvent event() {
        return event;
    }

    public boolean isPublished() {
        return published;
    }

    public void markPublished() {
        this.published = true;
    }
}
