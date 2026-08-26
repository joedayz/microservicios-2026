# 04. Spring Security OAuth2 Resource Server

## Qué se ve en `order-service-spring`

Spring hace dos capas distintas:

1. **autenticación técnica**: validar el JWT;
2. **autorización funcional**: decidir si el usuario puede operar.

## Archivos importantes

| Archivo | Qué enseña |
|---------|------------|
| `security/SecurityConfig.java` | pipeline de seguridad |
| `security/RealmRoleConverter.java` | roles de Keycloak a `GrantedAuthority` |
| `security/TenantClaimAuthorizer.java` | ABAC por tenant |
| `security/RegionClaimAuthorizer.java` | ABAC por región |
| `api/OrderController.java` | `@PreAuthorize` combinando RBAC + ABAC |

## Regla interesante para clase

```java
@PreAuthorize("""
    hasAnyAuthority('ROLE_orders_writer', 'ROLE_orders_admin')
    and @tenantClaimAuthorizer.sameTenant(authentication, #tenantId)
    and @regionClaimAuthorizer.sameRegion(authentication, #request.shippingRegion)
    """)
```

Esa sola línea resume la arquitectura:

- rol correcto;
- tenant correcto;
- región correcta.

## Qué no hace Spring por ti

Spring valida el token, pero **no adivina** tus reglas de negocio.  
Por eso los authorizers viven en beans explícitos y no escondidos en un filtro genérico.
