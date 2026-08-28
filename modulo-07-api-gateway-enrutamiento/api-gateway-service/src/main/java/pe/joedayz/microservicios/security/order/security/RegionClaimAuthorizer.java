package pe.joedayz.microservicios.security.order.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RegionClaimAuthorizer {

    private final JwtClaimAccessor jwtClaimAccessor;

    public RegionClaimAuthorizer(JwtClaimAccessor jwtClaimAccessor) {
        this.jwtClaimAccessor = jwtClaimAccessor;
    }

    public boolean sameRegion(Authentication authentication, String requestedRegion) {
        if (requestedRegion == null || requestedRegion.isBlank()) {
            return false;
        }
        if (isPrivileged(authentication)) {
            return true;
        }
        String tokenRegion = jwtClaimAccessor.getClaim(authentication, "region");
        return normalize(tokenRegion).equals(normalize(requestedRegion));
    }

    private boolean isPrivileged(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_orders_admin"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
