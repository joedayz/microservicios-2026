# 05. Quarkus OIDC en microservicios

## Qué se ve en `inventory-service-quarkus`

Quarkus usa `quarkus-oidc` para convertir una API REST en un Resource Server con poca configuración.

## Archivos importantes

| Archivo | Qué enseña |
|---------|------------|
| `src/main/resources/application.properties` | issuer, client-id, roles y perfiles TLS |
| `api/InventoryResource.java` | protección pública con `@RolesAllowed` |
| `security/AccessPolicyService.java` | ABAC a partir de claims |
| `api/InternalInventoryResource.java` | separación entre API pública y API interna |

## Idea central

En Quarkus también hay dos niveles:

1. **framework security** con `@Authenticated` y `@RolesAllowed`;
2. **business authorization** con `tenant_id` y `region`.

## Mensaje didáctico

> Un `200 OK` requiere dos llaves:  
> el framework acepta tu identidad y el dominio acepta tu contexto.
