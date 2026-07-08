package pe.joedayz.microservicios.modulo01.patterns.outbox;

import pe.joedayz.microservicios.modulo01.messaging.EventBus;

/**
 * Proceso que lee la tabla {@code outbox} y publica los eventos pendientes a Kafka,
 * marcandolos como enviados. En produccion es un poller programado o un conector CDC
 * como Debezium leyendo el WAL de PostgreSQL.
 *
 * <p>Garantiza entrega at-least-once: por eso los consumidores deben ser IDEMPOTENTES.
 */
public class OutboxRelay {

    private final OutboxStore outbox;
    private final EventBus eventBus;

    public OutboxRelay(OutboxStore outbox, EventBus eventBus) {
        this.outbox = outbox;
        this.eventBus = eventBus;
    }

    /** Publica todos los pendientes (simula un tick del poller). */
    public int relayPending() {
        int count = 0;
        for (OutboxMessage message : outbox.pending()) {
            eventBus.publish(message.event());
            message.markPublished();
            count++;
        }
        return count;
    }
}
