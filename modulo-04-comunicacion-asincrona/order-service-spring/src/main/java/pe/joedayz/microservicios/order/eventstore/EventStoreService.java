package pe.joedayz.microservicios.order.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de Event Sourcing: apéndice de eventos + replay para reconstruir
 * el historial de un agregado. La proyección de lectura (tabla `orders`) se
 * mantiene por separado (ver {@code Order}) para consultas rápidas (CQRS),
 * pero el historial completo siempre puede recomputarse desde acá.
 */
@Service
public class EventStoreService {

    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;

    public EventStoreService(EventStoreRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public <T> void append(String aggregateId, String tenantId, T event) {
        long nextVersion = nextVersion(aggregateId);
        String payload = writeJson(event);
        repository.save(new StoredEvent(aggregateId, tenantId, event.getClass().getSimpleName(),
                payload, nextVersion));
    }

    public List<StoredEvent> history(String aggregateId) {
        return repository.findByAggregateIdOrderByVersionAsc(aggregateId);
    }

    private long nextVersion(String aggregateId) {
        List<StoredEvent> existing = repository.findByAggregateIdOrderByVersionAsc(aggregateId);
        AtomicLong max = new AtomicLong(0);
        existing.forEach(e -> max.set(Math.max(max.get(), e.getVersion())));
        return max.get() + 1;
    }

    private String writeJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento", e);
        }
    }
}
