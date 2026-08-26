package pe.joedayz.microservicios.security.order.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RealmRoleConverterTest {

    @Test
    void shouldExtractRolesAndScopes() {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "scope", "openid inventory.read",
                        "realm_access", Map.of("roles", List.of("orders_writer", "orders_admin"))));

        RealmRoleConverter converter = new RealmRoleConverter();

        java.util.Collection<GrantedAuthority> authorities = converter.convert(jwt);
        List<String> values = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(values.contains("ROLE_orders_writer"));
        assertTrue(values.contains("ROLE_orders_admin"));
        assertTrue(values.contains("SCOPE_inventory.read"));
    }
}
