package pe.joedayz.microservicios.security.order.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import pe.joedayz.microservicios.security.order.api.dto.InventoryCallPreviewResponse;
import pe.joedayz.microservicios.security.order.config.InventoryClientProperties;
import pe.joedayz.microservicios.security.order.config.VaultProperties;

class InventoryMtlsClientTest {

    @Test
    void shouldBuildPreviewUsingLocalConfigWhenVaultIsDisabled() {
        InventoryClientProperties inventoryClientProperties = new InventoryClientProperties();
        inventoryClientProperties.setBaseUrl("https://inventory.local:8444");
        inventoryClientProperties.setClientId("local-order-service");
        inventoryClientProperties.setKeyStorePath("../certs/order-service-client.p12");
        inventoryClientProperties.setTrustStorePath("../certs/platform-truststore.p12");
        inventoryClientProperties.setTimeout(Duration.ofSeconds(2));
        inventoryClientProperties.setSimulateCall(true);

        VaultProperties vaultProperties = new VaultProperties();
        vaultProperties.setEnabled(false);

        InventoryMtlsClient client = new InventoryMtlsClient(
                inventoryClientProperties,
                vaultProperties,
                new TlsMaterialLoader(),
                mock(VaultSecretClient.class));

        InventoryCallPreviewResponse response = client.previewSecureCall("tienda-deportes", "ZAP-RUN-42", "PE");

        assertTrue(response.previewMode());
        assertEquals("local-config", response.clientIdSource());
        assertEquals("local-order-service", response.clientIdValue());
    }

    @Test
    void shouldReadClientIdFromVaultWhenEnabled() {
        InventoryClientProperties inventoryClientProperties = new InventoryClientProperties();
        inventoryClientProperties.setSimulateCall(true);

        VaultProperties vaultProperties = new VaultProperties();
        vaultProperties.setEnabled(true);

        VaultSecretClient vaultSecretClient = mock(VaultSecretClient.class);
        when(vaultSecretClient.readSecretValue()).thenReturn("vault-order-service");

        InventoryMtlsClient client = new InventoryMtlsClient(
                inventoryClientProperties,
                vaultProperties,
                new TlsMaterialLoader(),
                vaultSecretClient);

        InventoryCallPreviewResponse response = client.previewSecureCall("tienda-deportes", "ZAP-RUN-42", "PE");

        assertEquals("vault", response.clientIdSource());
        assertEquals("vault-order-service", response.clientIdValue());
    }
}
