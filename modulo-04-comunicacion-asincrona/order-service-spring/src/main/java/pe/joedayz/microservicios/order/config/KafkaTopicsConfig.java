package pe.joedayz.microservicios.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Order Service es dueño (producer) del topic "reserve-stock-command":
 * declara su creación con las partitions/replicas esperadas en vez de
 * depender de auto.create.topics.enable en producción.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic reserveStockCommandTopic() {
        return TopicBuilder.name("reserve-stock-command")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
