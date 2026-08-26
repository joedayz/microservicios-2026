package pe.joedayz.microservicios.security.order.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import pe.joedayz.microservicios.security.order.api.dto.InventoryCallPreviewResponse;
import pe.joedayz.microservicios.security.order.config.InventoryClientProperties;
import pe.joedayz.microservicios.security.order.config.VaultProperties;

@Component
public class InventoryMtlsClient {

    private final InventoryClientProperties inventoryClientProperties;
    private final VaultProperties vaultProperties;
    private final TlsMaterialLoader tlsMaterialLoader;
    private final VaultSecretClient vaultSecretClient;

    public InventoryMtlsClient(InventoryClientProperties inventoryClientProperties,
                               VaultProperties vaultProperties,
                               TlsMaterialLoader tlsMaterialLoader,
                               VaultSecretClient vaultSecretClient) {
        this.inventoryClientProperties = inventoryClientProperties;
        this.vaultProperties = vaultProperties;
        this.tlsMaterialLoader = tlsMaterialLoader;
        this.vaultSecretClient = vaultSecretClient;
    }

    public InventoryCallPreviewResponse previewSecureCall(String tenantId, String sku, String region) {
        String clientIdValue = resolveClientId();
        String targetUri = buildTargetUri(tenantId, sku, region);
        if (inventoryClientProperties.isSimulateCall()) {
            return new InventoryCallPreviewResponse(
                    tenantId,
                    sku,
                    region,
                    "mTLS",
                    targetUri,
                    true,
                    vaultProperties.isEnabled() ? "vault" : "local-config",
                    clientIdValue,
                    inventoryClientProperties.getKeyStorePath(),
                    inventoryClientProperties.getTrustStorePath(),
                    "Preview didactico: habilita module6.inventory.simulate-call=false para ejecutar el GET real.");
        }

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(inventoryClientProperties.getTimeout())
                    .sslContext(tlsMaterialLoader.createSslContext(
                            inventoryClientProperties.getKeyStorePath(),
                            inventoryClientProperties.getKeyStorePassword(),
                            inventoryClientProperties.getTrustStorePath(),
                            inventoryClientProperties.getTrustStorePassword()))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .timeout(inventoryClientProperties.getTimeout())
                    .header("X-Client-Id", clientIdValue)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new InventoryCallPreviewResponse(
                    tenantId,
                    sku,
                    region,
                    "mTLS",
                    targetUri,
                    false,
                    vaultProperties.isEnabled() ? "vault" : "local-config",
                    clientIdValue,
                    inventoryClientProperties.getKeyStorePath(),
                    inventoryClientProperties.getTrustStorePath(),
                    response.body());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo ejecutar la llamada mTLS hacia inventory-service", ex);
        }
    }

    private String buildTargetUri(String tenantId, String sku, String region) {
        return inventoryClientProperties.getBaseUrl()
                + "/internal/v1/tenants/"
                + url(tenantId)
                + "/inventory/"
                + url(sku)
                + "?region="
                + url(region);
    }

    private String resolveClientId() {
        if (vaultProperties.isEnabled()) {
            return vaultSecretClient.readSecretValue();
        }
        if (inventoryClientProperties.getClientId() == null || inventoryClientProperties.getClientId().isBlank()) {
            throw new IllegalStateException("module6.inventory.client-id es obligatorio cuando Vault esta deshabilitado");
        }
        return inventoryClientProperties.getClientId();
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
