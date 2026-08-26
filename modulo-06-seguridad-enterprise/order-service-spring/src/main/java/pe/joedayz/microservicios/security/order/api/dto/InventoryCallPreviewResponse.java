package pe.joedayz.microservicios.security.order.api.dto;

public record InventoryCallPreviewResponse(
        String tenantId,
        String sku,
        String region,
        String transport,
        String targetUri,
        boolean previewMode,
        String clientIdSource,
        String clientIdValue,
        String clientCertificate,
        String trustStore,
        String responseBody) {
}
