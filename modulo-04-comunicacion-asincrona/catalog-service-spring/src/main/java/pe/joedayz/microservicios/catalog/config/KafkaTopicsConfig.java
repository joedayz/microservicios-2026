package pe.joedayz.microservicios.catalog.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Catalog Service es dueño (producer) de los topics de reply de la saga.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic stockReservedTopic() {
        return TopicBuilder.name("stock-reserved")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockReservationFailedTopic() {
        return TopicBuilder.name("stock-reservation-failed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
