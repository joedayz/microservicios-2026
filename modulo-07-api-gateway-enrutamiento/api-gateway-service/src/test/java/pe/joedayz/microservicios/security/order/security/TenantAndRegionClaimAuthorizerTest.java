package pe.joedayz.microservicios.security.order.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantAndRegionClaimAuthorizerTest {

    @Test
    void shouldAllowMatchingTenantAndRegion() {
        JwtAuthenticationToken authentication = authentication("tienda-deportes", "PE", List.of("ROLE_orders_writer"));

        TenantClaimAuthorizer tenantClaimAuthorizer = new TenantClaimAuthorizer(new JwtClaimAccessor());
        RegionClaimAuthorizer regionClaimAuthorizer = new RegionClaimAuthorizer(new JwtClaimAccessor());

        assertTrue(tenantClaimAuthorizer.sameTenant(authentication, "tienda-deportes"));
        assertTrue(regionClaimAuthorizer.sameRegion(authentication, "PE"));
        assertFalse(tenantClaimAuthorizer.sameTenant(authentication, "libreria-lima"));
        assertFalse(regionClaimAuthorizer.sameRegion(authentication, "US"));
    }

    @Test
    void shouldAllowAdminToCrossTenantAndRegion() {
        JwtAuthenticationToken authentication = authentication("tienda-deportes", "PE", List.of("ROLE_orders_admin"));

        TenantClaimAuthorizer tenantClaimAuthorizer = new TenantClaimAuthorizer(new JwtClaimAccessor());
        RegionClaimAuthorizer regionClaimAuthorizer = new RegionClaimAuthorizer(new JwtClaimAccessor());

        assertTrue(tenantClaimAuthorizer.sameTenant(authentication, "cualquier-tenant"));
        assertTrue(regionClaimAuthorizer.sameRegion(authentication, "LATAM"));
    }

    private JwtAuthenticationToken authentication(String tenantId, String region, List<String> authorities) {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("tenant_id", tenantId, "region", region));

        return new JwtAuthenticationToken(jwt, authorities.stream().map(SimpleGrantedAuthority::new).toList());
    }
}
