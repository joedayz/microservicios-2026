package pe.joedayz.microservicios.notification.tenant;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.mongodb.client.MongoClient;

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.stereotype.Component;

import pe.joedayz.microservicios.notification.config.NotificationMongoProperties;

@Component
public class TenantMongoTemplateFactory {

    private final MongoClient mongoClient;
    private final NotificationMongoProperties properties;
    private final ConcurrentMap<String, MongoTemplate> templates = new ConcurrentHashMap<>();

    public TenantMongoTemplateFactory(MongoClient mongoClient, NotificationMongoProperties properties) {
        this.mongoClient = mongoClient;
        this.properties = properties;
    }

    public MongoTemplate currentTenantTemplate() {
        String tenantId = TenantContext.require();
        String databaseName = properties.getDatabasePrefix() + "_" + TenantDatabaseNameResolver.normalize(tenantId);
        return templates.computeIfAbsent(databaseName, this::createTemplate);
    }

    private MongoTemplate createTemplate(String databaseName) {
        MongoDatabaseFactory databaseFactory = new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
        return new MongoTemplate(databaseFactory);
    }
}
