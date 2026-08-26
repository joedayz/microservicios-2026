package pe.joedayz.microservicios.security.inventory.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Verifica el identificador lógico del cliente interno.
 * En producción la identidad fuerte la da el certificado mTLS;
 * este chequeo agrega una capa didáctica y trazable para la demo.
 */
@ApplicationScoped
public class InternalClientPolicyService {

    @ConfigProperty(name = "module6.internal.expected-client-id", defaultValue = "order-service-mtls-client")
    String expectedClientId;

    public void checkInternalClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new ForbiddenException("Falta el header X-Client-Id del cliente interno");
        }
        if (!expectedClientId.equals(clientId.trim())) {
            throw new ForbiddenException("Cliente interno no autorizado: " + clientId);
        }
    }
}
