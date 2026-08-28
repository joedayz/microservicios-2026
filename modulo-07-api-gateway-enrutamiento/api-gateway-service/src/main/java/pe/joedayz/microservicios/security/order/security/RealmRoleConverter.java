package pe.joedayz.microservicios.security.order.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class RealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> authorities = new LinkedHashSet<>();
        extractScopes(jwt).forEach(scope -> authorities.add("SCOPE_" + scope));
        extractRealmRoles(jwt).forEach(role -> authorities.add("ROLE_" + role));
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private List<String> extractScopes(Jwt jwt) {
        Object scopeClaim = jwt.getClaims().get("scope");
        if (scopeClaim instanceof String scopeText && !scopeText.isBlank()) {
            return List.of(scopeText.split("\\s+"));
        }
        if (scopeClaim instanceof Collection<?> scopeValues) {
            return scopeValues.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }
        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof Collection<?> roleValues)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object roleValue : roleValues) {
            result.add(roleValue.toString());
        }
        return result;
    }
}
