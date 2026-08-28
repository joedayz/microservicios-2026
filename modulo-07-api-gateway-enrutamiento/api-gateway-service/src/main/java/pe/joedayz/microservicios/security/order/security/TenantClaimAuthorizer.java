package pe.joedayz.microservicios.security.order.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class TenantClaimAuthorizer {

    private final JwtClaimAccessor jwtClaimAccessor;

    public TenantClaimAuthorizer(JwtClaimAccessor jwtClaimAccessor) {
        this.jwtClaimAccessor = jwtClaimAccessor;
    }

    public boolean sameTenant(Authentication authentication, String requestedTenantId) {
        if (requestedTenantId == null || requestedTenantId.isBlank()) {
            return false;
        }
        if (isPrivileged(authentication)) {
            return true;
        }
        String tokenTenant = jwtClaimAccessor.getClaim(authentication, "tenant_id");
        return normalize(tokenTenant).equals(normalize(requestedTenantId));
    }

    private boolean isPrivileged(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_orders_admin"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
