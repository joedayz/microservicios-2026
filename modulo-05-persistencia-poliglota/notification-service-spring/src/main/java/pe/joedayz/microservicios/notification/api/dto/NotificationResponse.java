package pe.joedayz.microservicios.notification.api.dto;

import java.time.Instant;

import pe.joedayz.microservicios.notification.domain.NotificationMessage;

public record NotificationResponse(
        String id,
        String tenantId,
        String customerId,
        String channel,
        String subject,
        String body,
        String status,
        Instant createdAt
) {

    public static NotificationResponse from(NotificationMessage message) {
        return new NotificationResponse(
                message.getId(),
                message.getTenantId(),
                message.getCustomerId(),
                message.getChannel(),
                message.getSubject(),
                message.getBody(),
                message.getStatus(),
                message.getCreatedAt());
    }
}
