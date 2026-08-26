package pe.joedayz.microservicios.security.order.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimAccessor {

    public String getClaim(Authentication authentication, String claimName) {
        if (authentication instanceof JwtAuthenticationToken tokenAuthentication) {
            Object value = tokenAuthentication.getTokenAttributes().get(claimName);
            return value == null ? null : value.toString();
        }
        return null;
    }
}
