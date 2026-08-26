# 02. OAuth2 y OpenID Connect desde cero

## OAuth2 en una frase

OAuth2 responde: **“¿puede esta aplicación acceder a este recurso?”**

## OpenID Connect en una frase

OIDC agrega identidad: **“¿quién es el usuario autenticado?”**

## Piezas del protocolo

| Pieza | Rol en la demo |
|------|-----------------|
| Resource Owner | `ana-reader`, `bruno-manager`, `carla-admin` |
| Client | `student-portal` |
| Authorization Server | Keycloak |
| Resource Server | `order-service-spring`, `inventory-service-quarkus` |

## Flujo explicado sin magia

1. El usuario se autentica contra Keycloak.
2. Keycloak entrega un **access token** corto y un **refresh token** más durable.
3. El frontend manda el access token al microservicio.
4. El Resource Server valida firma, expiración, issuer y claims.
5. Si el token es válido, recién entra en juego la autorización del negocio.

## Qué agrega OIDC sobre OAuth2

- `id_token` para identidad del usuario;
- endpoint `userinfo`;
- discovery metadata (`/.well-known/openid-configuration`);
- estandarización de claims como `sub`, `preferred_username`, `email`.

## Error común que conviene corregir en clase

> OAuth2 **no define el formato del token**.  
> JWT es una implementación frecuente, no el protocolo mismo.
