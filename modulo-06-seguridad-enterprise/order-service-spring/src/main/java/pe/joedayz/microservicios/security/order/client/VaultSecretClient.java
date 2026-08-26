package pe.joedayz.microservicios.security.order.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import pe.joedayz.microservicios.security.order.config.VaultProperties;

@Component
public class VaultSecretClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final VaultProperties vaultProperties;
    private final ObjectMapper objectMapper;

    public VaultSecretClient(VaultProperties vaultProperties, ObjectMapper objectMapper) {
        this.vaultProperties = vaultProperties;
        this.objectMapper = objectMapper;
    }

    public String readSecretValue() {
        if (!vaultProperties.isEnabled()) {
            throw new IllegalStateException("Vault esta deshabilitado para este entorno");
        }
        if (vaultProperties.getToken() == null || vaultProperties.getToken().isBlank()) {
            throw new IllegalStateException("module6.vault.token es obligatorio cuando Vault esta habilitado");
        }

        String uri = vaultProperties.getAddress()
                + "/v1/"
                + vaultProperties.getKvMount()
                + "/data/"
                + vaultProperties.getPath();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("X-Vault-Token", vaultProperties.getToken())
                .GET()
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Vault respondio con estado " + response.statusCode());
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), MAP_TYPE);
            String fieldName = vaultProperties.getField();
            Object value = nestedValue(payload, fieldName);
            if (value == null) {
                throw new IllegalStateException("El secreto no contiene el campo esperado: " + fieldName);
            }
            return value.toString();
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo leer el secreto desde Vault", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object nestedValue(Map<String, Object> payload, String fieldName) {
        Object dataNode = payload.get("data");
        if (!(dataNode instanceof Map<?, ?> dataWrapper)) {
            return null;
        }
        Object secretNode = dataWrapper.get("data");
        if (!(secretNode instanceof Map<?, ?> secretData)) {
            return null;
        }
        return ((Map<String, Object>) secretData).get(fieldName);
    }
}
