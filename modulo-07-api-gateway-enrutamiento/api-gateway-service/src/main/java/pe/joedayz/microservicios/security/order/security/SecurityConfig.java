package pe.joedayz.microservicios.security.order.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    private final RealmRoleConverter realmRoleConverter;

    public SecurityConfig(RealmRoleConverter realmRoleConverter) {
        this.realmRoleConverter = realmRoleConverter;
    }

    @Bean
        SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(authorize -> authorize
                .pathMatchers("/actuator/health", "/actuator/info", "/actuator/gateway/**").permitAll()
                .pathMatchers("/fallback/**", "/gateway/admin/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                jwt -> jwt.jwtAuthenticationConverter(
                    new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(realmRoleConverter);
        return converter;
    }
}
