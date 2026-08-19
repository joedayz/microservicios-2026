package pe.joedayz.microservicios.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotBlank @Size(max = 60) String customerId,
        @NotBlank @Size(max = 20) String channel,
        @NotBlank @Size(max = 120) String subject,
        @NotBlank @Size(max = 500) String body
) {
}
