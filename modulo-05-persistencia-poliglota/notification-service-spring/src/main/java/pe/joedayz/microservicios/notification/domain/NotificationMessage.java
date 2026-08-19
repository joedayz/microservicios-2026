package pe.joedayz.microservicios.notification.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
public class NotificationMessage {

    @Id
    private String id;

    private String tenantId;
    private String customerId;
    private String channel;
    private String subject;
    private String body;
    private String status;
    private Instant createdAt;

    public NotificationMessage() {
    }

    public NotificationMessage(String id,
                               String tenantId,
                               String customerId,
                               String channel,
                               String subject,
                               String body,
                               String status,
                               Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
