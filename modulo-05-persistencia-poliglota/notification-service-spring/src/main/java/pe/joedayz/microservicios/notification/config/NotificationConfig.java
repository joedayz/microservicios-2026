package pe.joedayz.microservicios.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationMongoProperties.class)
public class NotificationConfig {
}
