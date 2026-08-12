package pe.joedayz.microservicios.catalog.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CDC poller simplificado del patrón Transactional Outbox: cada
 * {@code poll-delay-ms} lee filas no publicadas y las envía a Kafka.
 * Si el proceso cae entre el envío y el commit del "published_at", el
 * próximo ciclo reintenta (at-least-once delivery); los consumers deben
 * ser idempotentes.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxPublisher(OutboxRepository outboxRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            @org.springframework.beans.factory.annotation.Value("${catalog.outbox.batch-size:20}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${catalog.outbox.poll-delay-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedAtIsNullOrderById(PageRequest.of(0, batchSize));
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished();
                log.info("Outbox event {} publicado en topic {}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Fallo publicando outbox event {}, se reintentará en el siguiente ciclo", event.getId(), e);
            }
        }
    }
}
