package pe.joedayz.microservicios.order.eventstore;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);
}
