package pe.joedayz.microservicios.security.order.api.dto;

import java.util.List;

public record SecurityReportResponse(
        String username,
        String tenantId,
        String region,
        List<String> authorities) {
}
