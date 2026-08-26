# 08. Guía didáctica paso a paso

Esta guía está pensada para dictar la clase mostrando una historia completa:
**login → token → autorización → llamada interna segura → secretos externos**.

## 1. Historia que cuenta el módulo

En módulos anteriores ya resolvimos:

- APIs síncronas;
- eventos;
- persistencia;
- multi-tenancy.

Ahora respondemos la pregunta:

> **¿Cómo evitamos que cualquier usuario o cualquier servicio toque información que no le corresponde?**

## 2. Orden recomendado para explicarlo

1. Keycloak como casa central de identidad.
2. OAuth2 / OIDC como protocolo.
3. JWT por dentro.
4. Spring Resource Server.
5. Quarkus OIDC.
6. RBAC vs ABAC.
7. mTLS.
8. Vault.

## 3. Demo en vivo

### Paso 1: levantar infraestructura

```bash
cd modulo-06-seguridad-enterprise/docker-compose
docker compose up -d
```

### Paso 2: generar certificados

```bash
cd modulo-06-seguridad-enterprise
./scripts/01-generate-certs.sh
```

### Paso 3: sembrar secreto en Vault

```bash
cd modulo-06-seguridad-enterprise
./scripts/02-vault-seed.sh
```

### Paso 4: ejecutar Spring y Quarkus

```bash
cd modulo-06-seguridad-enterprise/order-service-spring
mvn spring-boot:run
```

```bash
cd modulo-06-seguridad-enterprise/inventory-service-quarkus
mvn quarkus:dev -Dquarkus.profile=mtls
```

### Paso 5: pedir token de un lector

Usa `ana-reader` para demostrar lectura autorizada y escritura denegada.

### Paso 6: pedir token de un manager

Usa `bruno-manager` para demostrar escritura permitida solo dentro de `tienda-deportes` y `PE`.

### Paso 7: mostrar un `403`

Cambia:

- el tenant de la URL;
- o la región del body/query;
- o usa un rol insuficiente.

Ese momento enseña mejor ABAC que diez slides.

## 4. Recorrido archivo por archivo

### Spring

1. `SecurityConfig.java`
2. `RealmRoleConverter.java`
3. `TenantClaimAuthorizer.java`
4. `RegionClaimAuthorizer.java`
5. `InventoryMtlsClient.java`
6. `VaultSecretClient.java`

### Quarkus

1. `application.properties`
2. `InventoryResource.java`
3. `AccessPolicyService.java`
4. `InternalInventoryResource.java`
5. `InternalClientPolicyService.java`

## 5. Idea final para cerrar la clase

> Seguridad enterprise no es “poner un login”.  
> Es separar identidad, autorización, transporte y secretos en capas explícitas.
