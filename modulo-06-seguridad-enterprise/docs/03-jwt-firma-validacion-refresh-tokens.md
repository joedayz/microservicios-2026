# 03. JWT: firma, validación y refresh tokens

## Anatomía de un JWT

```text
xxxxx.yyyyy.zzzzz
header.payload.signature
```

## Qué valida un Resource Server serio

1. **firma**: el token fue emitido por el IdP esperado;
2. **exp**: no está expirado;
3. **iss**: viene del issuer correcto;
4. **aud** o el client esperado cuando aplique;
5. claims de negocio como `tenant_id`, `region`, `scope` o roles.

## Access token vs refresh token

| Token | Uso | Vida típica |
|------|-----|-------------|
| Access token | llamar APIs | corta |
| Refresh token | pedir un nuevo access token | media/larga |

## Qué enseñar con esta demo

- `RealmRoleConverter` en Spring muestra cómo traducir el claim `realm_access.roles`.
- `AccessPolicyService` en Quarkus muestra que un token válido **todavía** puede ser rechazado por negocio.

## Ejemplo de refresh token

```bash
curl -X POST 'http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=refresh_token' \
  -d 'client_id=student-portal' \
  -d 'refresh_token=<refresh-token>'
```

## Mensaje didáctico

> Validar la firma del JWT te dice que el token es auténtico.  
> No te dice automáticamente que el usuario pueda tocar **ese tenant** o **esa región**.
