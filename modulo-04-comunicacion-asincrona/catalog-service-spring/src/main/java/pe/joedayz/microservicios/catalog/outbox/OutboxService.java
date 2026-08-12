package pe.joedayz.microservicios.catalog.outbox;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * Registra eventos en la tabla outbox dentro de la transacción del caller
 * (no abre su propia transacción: hereda la del método que la invoca).
 */
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public <T> void append(String topic, String messageKey, String aggregateId, String tenantId, T event) {
        String payload = writeJson(event);
        outboxRepository.save(new OutboxEvent(aggregateId, tenantId, event.getClass().getSimpleName(),
                topic, messageKey, payload));
    }

    private String writeJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento de outbox", e);
        }
    }
}
