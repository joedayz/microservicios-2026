package pe.joedayz.microservicios.modulo01.patterns.outbox;

import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa la tabla {@code outbox}. Se escribe en la MISMA transaccion que el cambio
 * de negocio (aqui simulado): asi el evento se persiste atomicamente junto al pedido.
 */
public class OutboxStore {

    private final List<OutboxMessage> messages = new ArrayList<>();

    public void add(DomainEvent event) {
        messages.add(new OutboxMessage(event));
    }

    public List<OutboxMessage> pending() {
        List<OutboxMessage> result = new ArrayList<>();
        for (OutboxMessage m : messages) {
            if (!m.isPublished()) {
                result.add(m);
            }
        }
        return result;
    }
}
