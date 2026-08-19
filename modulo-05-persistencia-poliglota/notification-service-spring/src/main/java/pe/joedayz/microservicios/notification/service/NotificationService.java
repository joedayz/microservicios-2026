package pe.joedayz.microservicios.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import pe.joedayz.microservicios.notification.api.dto.NotificationRequest;
import pe.joedayz.microservicios.notification.api.dto.NotificationResponse;
import pe.joedayz.microservicios.notification.domain.NotificationMessage;
import pe.joedayz.microservicios.notification.tenant.TenantContext;
import pe.joedayz.microservicios.notification.tenant.TenantMongoTemplateFactory;

@Service
public class NotificationService {

    private final TenantMongoTemplateFactory mongoTemplateFactory;

    public NotificationService(TenantMongoTemplateFactory mongoTemplateFactory) {
        this.mongoTemplateFactory = mongoTemplateFactory;
    }

    public List<NotificationResponse> listNotifications() {
        MongoTemplate template = mongoTemplateFactory.currentTenantTemplate();
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return template.find(query, NotificationMessage.class).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        MongoTemplate template = mongoTemplateFactory.currentTenantTemplate();
        NotificationMessage message = new NotificationMessage(
                UUID.randomUUID().toString(),
                TenantContext.require(),
                request.customerId(),
                request.channel(),
                request.subject(),
                request.body(),
                "PENDING",
                Instant.now());
        return NotificationResponse.from(template.save(message));
    }
}
