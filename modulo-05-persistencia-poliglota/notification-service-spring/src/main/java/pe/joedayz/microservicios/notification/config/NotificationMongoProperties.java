package pe.joedayz.microservicios.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.mongo")
public class NotificationMongoProperties {

    private String databasePrefix = "notifications";

    public String getDatabasePrefix() {
        return databasePrefix;
    }

    public void setDatabasePrefix(String databasePrefix) {
        this.databasePrefix = databasePrefix;
    }
}
