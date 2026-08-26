package pe.joedayz.microservicios.security.inventory.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * ABAC: reglas de acceso basadas en atributos (claims) del token.
 * Un token con firma válida todavía puede ser rechazado aquí
 * si intenta operar sobre otro tenant u otra región.
 */
@ApplicationScoped
public class AccessPolicyService {

    private static final String ADMIN_ROLE = "inventory_admin";

    @Inject
    SecurityIdentity securityIdentity;

    public void checkTenantAccess(String requestedTenantId) {
        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            throw new ForbiddenException("El tenant solicitado es obligatorio");
        }
        if (securityIdentity.hasRole(ADMIN_ROLE)) {
            return;
        }
        String tokenTenant = claim("tenant_id");
        if (!normalizeLower(tokenTenant).equals(normalizeLower(requestedTenantId))) {
            throw new ForbiddenException("El token no autoriza el tenant " + requestedTenantId);
        }
    }

    public void checkRegionAccess(String requestedRegion) {
        if (requestedRegion == null || requestedRegion.isBlank()) {
            throw new ForbiddenException("La region solicitada es obligatoria");
        }
        if (securityIdentity.hasRole(ADMIN_ROLE)) {
            return;
        }
        String tokenRegion = claim("region");
        if (!normalizeUpper(tokenRegion).equals(normalizeUpper(requestedRegion))) {
            throw new ForbiddenException("El token no autoriza la region " + requestedRegion);
        }
    }

    private String claim(String claimName) {
        if (securityIdentity.getPrincipal() instanceof JsonWebToken token) {
            Object value = token.getClaim(claimName);
            return value == null ? "" : value.toString();
        }
        return "";
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
