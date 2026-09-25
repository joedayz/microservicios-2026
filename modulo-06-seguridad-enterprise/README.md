# Módulo 6 – Workshop de seguridad enterprise

> **Keycloak · OAuth2/OIDC · JWT · Spring Security · Quarkus OIDC · RBAC/ABAC · mTLS · Vault**
>
> Java 21 · Spring Boot 4 · Quarkus 3

Este workshop protege dos APIs del e-commerce. Keycloak emite los JWT, Spring
Security y Quarkus OIDC los validan, RBAC y ABAC deciden el acceso, Vault
entrega un identificador interno y mTLS autentica el canal entre servicios.

```text
Usuario ──token──> Keycloak :8180
   ├──Bearer JWT──> order-service-spring :8086
   └──Bearer JWT──> inventory-service-quarkus :8084

order-service ──mTLS + X-Client-Id──> inventory-service :8444
      └──lee client-id──> Vault :8200
```

Al finalizar habrás obtenido y decodificado tokens, comprobado RBAC y ABAC con
tres usuarios, distinguido `401` de `403`, inspeccionado Keycloak y Vault, y
ejecutado tanto el preview como la variante real del flujo mTLS.

## 0. Prerrequisitos

Necesitas:

- JDK 21 o superior y Maven 3.9 o superior.
- Docker Desktop en Windows/macOS, Docker o Podman en Linux.
- Puertos libres: `8084`, `8086`, `8180`, `8200` y `8444`.
- macOS/Linux/Git Bash: Bash, `curl`, `jq`, OpenSSL y `keytool`.
- Windows PowerShell: PowerShell 7+, OpenSSL y `keytool` en `PATH`.

`keytool` forma parte del JDK. Docker y Podman son alternativas: usa uno solo.
Ejecuta los comandos desde la raíz del repositorio `microservicios-2026`, salvo
que se indique otra terminal.

### Comprobar herramientas

**macOS / Linux / Git Bash**

```bash
java -version
mvn -version
docker version
docker compose version
curl --version
jq --version
openssl version
keytool -help >/dev/null
```

**Windows PowerShell**

```powershell
java -version
mvn -version
docker version
docker compose version
$PSVersionTable.PSVersion
curl.exe --version
openssl version
keytool -help
```

> Los tres archivos de [`scripts/`](scripts/) son Bash, no PowerShell nativo.
> Funcionan desde macOS/Linux/Git Bash si las herramientas están en `PATH`.
> Las alternativas PowerShell necesarias aparecen en este workshop.

## 1. Levantar Keycloak y Vault

El compose usa Keycloak `26.0.5` y Vault `1.19` en modo desarrollo. Abre la
**Terminal 1**.

**Docker (Windows/macOS/Linux)**

```bash
cd modulo-06-seguridad-enterprise/docker-compose
docker compose up -d
docker compose ps
```

**Podman (macOS/Linux)**

```bash
cd modulo-06-seguridad-enterprise/docker-compose
podman compose up -d
podman compose ps
```

Debes ver `modulo6-keycloak` y `modulo6-vault`. Espera a que Keycloak termine
de importar el realm.

### Verificar la infraestructura

**macOS / Linux / Git Bash**

```bash
curl -fsS http://localhost:8180/realms/joedayz-microservices | jq '.realm'
curl -fsS http://localhost:8200/v1/sys/health | jq '{initialized,sealed,standby}'
```

**Windows PowerShell**

```powershell
(Invoke-RestMethod http://localhost:8180/realms/joedayz-microservices).realm
Invoke-RestMethod http://localhost:8200/v1/sys/health |
  Select-Object initialized,sealed,standby | ConvertTo-Json
```

Resultados esperados: realm `joedayz-microservices`, Vault inicializado y
`sealed: false`.

## 2. Recorrido web de Keycloak

Abre [http://localhost:8180/admin](http://localhost:8180/admin).

- Usuario: `admin`
- Contraseña: `admin`

Este acceso y estas credenciales son solo del laboratorio. En el selector de
realm elige **joedayz-microservices** y recorre:

1. **Realm roles**: `orders_reader`, `orders_writer`, `orders_admin`,
   `inventory_viewer`, `inventory_manager` e `inventory_admin`.
2. **Clients → student-portal**: cliente público con Direct access grants y
   mappers `tenant-id` y `region`.
3. **Users**: `ana-reader`, `bruno-manager` y `carla-admin`; abre **Role
   mapping** y **Attributes** para relacionar roles, tenant y región.

La fuente importada es
[`joedayz-microservices-realm.json`](docker-compose/keycloak/realm-export/joedayz-microservices-realm.json).
No cambies la consola durante el workshop: al recrear el contenedor se vuelve a
importar el archivo del repositorio.

## 3. Recorrido web de Vault y siembra

La imagen de Vault incluye UI y el servidor dev la expone en
[http://localhost:8200/ui](http://localhost:8200/ui). Elige **Token** e ingresa
`root`. Si la UI no carga en tu distribución, usa la API de los siguientes
comandos: es la interfaz que consume realmente el código.

Vault está en modo dev: token raíz conocido, datos en memoria y sin TLS. No es
una configuración de producción. Cualquier recreación o reinicio pierde el
secreto.

### Sembrar desde Bash

```bash
cd modulo-06-seguridad-enterprise
./scripts/02-vault-seed.sh
```

El script llama a KV v2 y crea
`secret/data/module6/inventory-client` con
`client-id=order-service-mtls-client`.

### Sembrar desde Windows PowerShell

```powershell
$Headers = @{ 'X-Vault-Token' = 'root' }
$Body = @{ data = @{ 'client-id' = 'order-service-mtls-client' } } |
  ConvertTo-Json -Depth 3
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8200/v1/secret/data/module6/inventory-client `
  -Headers $Headers -ContentType 'application/json' -Body $Body
```

Verifica la lectura:

**macOS / Linux / Git Bash**

```bash
curl -fsS -H 'X-Vault-Token: root' \
  http://localhost:8200/v1/secret/data/module6/inventory-client |
  jq '.data.data'
```

**Windows PowerShell**

```powershell
(Invoke-RestMethod `
  -Uri http://localhost:8200/v1/secret/data/module6/inventory-client `
  -Headers $Headers).data.data | ConvertTo-Json
```

En la UI abre **Secrets engines → secret → module6 → inventory-client**. La
política incluida en
[`module6-order-service.hcl`](docker-compose/vault/policies/module6-order-service.hcl)
describe permiso de lectura, pero el compose no la carga ni crea un token
limitado: la demo usa directamente `root`.

## 4. Generar certificados

El script elimina y regenera el material PKCS#12 dentro de `certs/`; requiere
Bash, OpenSSL y `keytool`.

**macOS / Linux / Git Bash**

```bash
cd modulo-06-seguridad-enterprise
./scripts/01-generate-certs.sh
keytool -list -keystore certs/platform-truststore.p12 -storepass changeit
```

En PowerShell puro no existe un script equivalente en el repositorio. La
alternativa soportada es ejecutar el `.sh` desde Git Bash. También puedes
invocarlo desde PowerShell si Git for Windows está instalado:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' `
  .\modulo-06-seguridad-enterprise\scripts\01-generate-certs.sh
keytool -list -keystore `
  .\modulo-06-seguridad-enterprise\certs\platform-truststore.p12 `
  -storepass changeit
```

No se ofrece una traducción PowerShell inventada: el script contiene toda la
secuencia de CA, CSR, firmas, PKCS#12 y truststore que debe conservarse.

> Limitación real: el certificado de inventory solo tiene
> `CN=inventory-service`; no tiene SAN para `localhost`. El preview funciona sin
> red. Para la llamada real se usará el hostname `inventory-service`.

## 5. Iniciar los servicios

Abre la **Terminal 2** y deja Inventory ejecutándose:

```bash
cd modulo-06-seguridad-enterprise/inventory-service-quarkus
mvn quarkus:dev
```

Abre la **Terminal 3** y deja Order ejecutándose:

```bash
cd modulo-06-seguridad-enterprise/order-service-spring
mvn spring-boot:run
```

Comprueba salud.

**macOS / Linux / Git Bash**

```bash
curl -fsS http://localhost:8084/q/health/ready | jq
curl -fsS http://localhost:8086/actuator/health | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8084/q/health/ready |
  ConvertTo-Json -Depth 5
Invoke-RestMethod http://localhost:8086/actuator/health |
  ConvertTo-Json -Depth 5
```

Ambos deben mostrar `UP`.

## 6. Obtener y decodificar un token

El realm trae estos usuarios; todos usan `secret123`:

- `ana-reader`: lectura, tenant `tienda-deportes`, región `PE`.
- `bruno-manager`: lectura/escritura, mismo tenant y región.
- `carla-admin`: administración, tenant `plataforma`, región `LATAM`.

El laboratorio usa Resource Owner Password Credentials porque permite observar
tokens desde terminal. Es un flujo legado; una aplicación real debe preferir
Authorization Code + PKCE.

### macOS / Linux / Git Bash

```bash
TOKEN_RESPONSE=$(curl -fsS -X POST \
  http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=student-portal' \
  -d 'username=ana-reader' \
  -d 'password=secret123')

ACCESS_TOKEN=$(jq -r '.access_token' <<<"$TOKEN_RESPONSE")
echo "$TOKEN_RESPONSE" | jq '{token_type,expires_in,refresh_expires_in}'
echo "$ACCESS_TOKEN" | cut -d. -f2 |
  awk '{l=length($0)%4; if(l==2)$0=$0"=="; else if(l==3)$0=$0"="; print}' |
  tr '_-' '/+' | base64 --decode | jq \
  '{sub,preferred_username,iss,exp,realm_access,tenant_id,region}'
```

### Windows PowerShell

```powershell
$TokenResponse = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body @{
    grant_type = 'password'
    client_id  = 'student-portal'
    username   = 'ana-reader'
    password   = 'secret123'
  }
$AccessToken = $TokenResponse.access_token
$TokenResponse | Select-Object token_type,expires_in,refresh_expires_in

$Payload = $AccessToken.Split('.')[1].Replace('-','+').Replace('_','/')
switch ($Payload.Length % 4) {
  2 { $Payload += '==' }
  3 { $Payload += '=' }
}
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Payload)) |
  ConvertFrom-Json | Select-Object sub,preferred_username,iss,exp,
  realm_access,tenant_id,region | ConvertTo-Json -Depth 6
```

Decodificar solo permite leer el payload; **no valida la firma**. Los servicios
validan firma, issuer y expiración mediante la configuración OIDC.

## 7. RBAC y ABAC en Order Service

Primero identifica al usuario autenticado.

**macOS / Linux / Git Bash**

```bash
curl -fsS -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8086/api/v1/security/me | jq
```

**Windows PowerShell**

```powershell
$Auth = @{ Authorization = "Bearer $AccessToken" }
Invoke-RestMethod http://localhost:8086/api/v1/security/me -Headers $Auth |
  ConvertTo-Json -Depth 6
```

### Ana: lectura permitida, escritura denegada por RBAC

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8086/api/v1/tenants/tienda-deportes/orders

curl -i -X POST \
  http://localhost:8086/api/v1/tenants/tienda-deportes/orders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sku":"ZAP-RUN-42","quantity":2,"shippingRegion":"PE"}'
```

En PowerShell usa:

```powershell
Invoke-RestMethod `
  http://localhost:8086/api/v1/tenants/tienda-deportes/orders `
  -Headers $Auth
try {
  Invoke-RestMethod -Method Post `
    http://localhost:8086/api/v1/tenants/tienda-deportes/orders `
    -Headers $Auth -ContentType 'application/json' `
    -Body '{"sku":"ZAP-RUN-42","quantity":2,"shippingRegion":"PE"}'
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

El GET responde `200`; el POST responde `403` porque Ana no tiene
`orders_writer`.

### Bruno: escritura permitida y ABAC aplicado

Obtén otro token repitiendo el paso 6 con `bruno-manager`; guárdalo como
`BRUNO_TOKEN` en Bash o `$BrunoToken` en PowerShell.

```bash
BRUNO_TOKEN=$(curl -fsS -X POST \
  http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=password -d client_id=student-portal \
  -d username=bruno-manager -d password=secret123 |
  jq -r '.access_token')

curl -i -X POST \
  http://localhost:8086/api/v1/tenants/tienda-deportes/orders \
  -H "Authorization: Bearer $BRUNO_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sku":"ZAP-RUN-42","quantity":2,"shippingRegion":"PE"}'
```

```powershell
$BrunoToken = (Invoke-RestMethod -Method Post `
  -Uri http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body @{
    grant_type='password'; client_id='student-portal'
    username='bruno-manager'; password='secret123'
  }).access_token
$BrunoAuth = @{ Authorization = "Bearer $BrunoToken" }

Invoke-RestMethod -Method Post `
  http://localhost:8086/api/v1/tenants/tienda-deportes/orders `
  -Headers $BrunoAuth -ContentType 'application/json' `
  -Body '{"sku":"ZAP-RUN-42","quantity":2,"shippingRegion":"PE"}'
```

El resultado es `201`. Ahora cambia solo un atributo:

```bash
# tenant ajeno
curl -i -H "Authorization: Bearer $BRUNO_TOKEN" \
  http://localhost:8086/api/v1/tenants/libreria-lima/orders

# región ajena
curl -i -X POST \
  http://localhost:8086/api/v1/tenants/tienda-deportes/orders \
  -H "Authorization: Bearer $BRUNO_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sku":"ZAP-RUN-42","quantity":1,"shippingRegion":"CL"}'
```

```powershell
try {
  Invoke-RestMethod `
    http://localhost:8086/api/v1/tenants/libreria-lima/orders `
    -Headers $BrunoAuth
} catch {
  $_.Exception.Response.StatusCode.value__
}
try {
  Invoke-RestMethod -Method Post `
    http://localhost:8086/api/v1/tenants/tienda-deportes/orders `
    -Headers $BrunoAuth -ContentType 'application/json' `
    -Body '{"sku":"ZAP-RUN-42","quantity":1,"shippingRegion":"CL"}'
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

Ambas peticiones responden `403`: Bruno sí tiene el rol, pero los claims
`tenant_id` y `region` no autorizan esos recursos. Los roles admin omiten estas
dos comparaciones en el código.

### Script Bash equivalente

```bash
cd modulo-06-seguridad-enterprise
./scripts/03-token-demo.sh ana-reader
./scripts/03-token-demo.sh bruno-manager
```

No hay versión `.ps1` de este script.

## 8. Probar Inventory Service con el mismo JWT

Los roles de realm también se usan en Quarkus.

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8084/api/v1/tenants/tienda-deportes/inventory

curl -i -X POST \
  http://localhost:8084/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42/reserve \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"quantity":1,"region":"PE"}'
```

Ana obtiene `200` al leer y `403` al reservar. Repite el POST con el token de
Bruno: obtiene `200`.

```powershell
Invoke-RestMethod `
  http://localhost:8084/api/v1/tenants/tienda-deportes/inventory `
  -Headers $Auth | ConvertTo-Json -Depth 5

try {
  Invoke-RestMethod -Method Post `
    http://localhost:8084/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42/reserve `
    -Headers $Auth -ContentType 'application/json' `
    -Body '{"quantity":1,"region":"PE"}'
} catch {
  $_.Exception.Response.StatusCode.value__
}

Invoke-RestMethod -Method Post `
  http://localhost:8084/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42/reserve `
  -Headers $BrunoAuth -ContentType 'application/json' `
  -Body '{"quantity":1,"region":"PE"}' |
  ConvertTo-Json -Depth 5
```

## 9. Distinguir 401 de 403

```bash
# Sin credenciales: 401
curl -i http://localhost:8086/api/v1/security/me

# Token corrupto o vencido: 401
curl -i -H 'Authorization: Bearer no-es-un-jwt' \
  http://localhost:8086/api/v1/security/me

# Token válido, permiso insuficiente: 403
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8086/api/v1/admin/orders/report
```

**Windows PowerShell**

```powershell
try {
  Invoke-WebRequest http://localhost:8086/api/v1/security/me
} catch {
  $_.Exception.Response.StatusCode.value__ # 401
}
try {
  Invoke-WebRequest http://localhost:8086/api/v1/admin/orders/report `
    -Headers $Auth
} catch {
  $_.Exception.Response.StatusCode.value__ # 403
}
```

- `401 Unauthorized`: no existe una identidad autenticada válida.
- `403 Forbidden`: la identidad es válida, pero RBAC o ABAC rechaza la acción.

Spring Security responde normalmente sin body en estos casos; usa `-i` para
ver el estado. Los access tokens del realm duran 300 segundos. Si una prueba
que antes funcionaba pasa a `401`, solicita uno nuevo.

## 10. Preview de Vault y mTLS

Este endpoint exige `ROLE_orders_admin` o el scope `inventory.read`; ninguno de
los usuarios demo tiene ese scope. Obtén un token de Carla.

**macOS / Linux / Git Bash**

```bash
ADMIN_TOKEN=$(curl -fsS -X POST \
  http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=password -d client_id=student-portal \
  -d username=carla-admin -d password=secret123 |
  jq -r '.access_token')

curl -fsS -H "Authorization: Bearer $ADMIN_TOKEN" \
  'http://localhost:8086/api/v1/tenants/plataforma/orders/inventory-check/ZAP-RUN-42?region=LATAM' |
  jq
```

**Windows PowerShell**

```powershell
$AdminResponse = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body @{
    grant_type='password'; client_id='student-portal'
    username='carla-admin'; password='secret123'
  }
$AdminToken = $AdminResponse.access_token
$AdminAuth = @{ Authorization = "Bearer $AdminToken" }
Invoke-RestMethod `
  'http://localhost:8086/api/v1/tenants/plataforma/orders/inventory-check/ZAP-RUN-42?region=LATAM' `
  -Headers $AdminAuth | ConvertTo-Json -Depth 6
```

La respuesta muestra `previewMode: true`: todavía no sale una petición de red.
También muestra `clientIdSource: local-config`, el keystore cliente y el
truststore que usaría.

Para comprobar Vault en el preview, detén Order con `Ctrl+C` y reinícialo.

**macOS / Linux / Git Bash**

```bash
cd modulo-06-seguridad-enterprise/order-service-spring
VAULT_ENABLED=true mvn spring-boot:run
```

**Windows PowerShell**

```powershell
cd modulo-06-seguridad-enterprise/order-service-spring
$env:VAULT_ENABLED = 'true'
mvn spring-boot:run
```

Repite la llamada con un token nuevo si expiró. Ahora debe aparecer
`clientIdSource: vault` y `clientIdValue: order-service-mtls-client`.

## 11. mTLS real opcional

Este paso sí ejecuta la llamada. Primero detén Inventory y Order con `Ctrl+C`.

### 11.1 Resolver el hostname del certificado

El certificado no sirve para `localhost`; agrega temporalmente
`127.0.0.1 inventory-service` al archivo hosts.

**macOS / Linux**

```bash
echo '127.0.0.1 inventory-service' | sudo tee -a /etc/hosts
```

**Windows PowerShell como Administrador**

```powershell
Add-Content $env:SystemRoot\System32\drivers\etc\hosts `
  "`r`n127.0.0.1 inventory-service"
```

En Git Bash sobre Windows realiza el mismo cambio desde un editor elevado. Si
no deseas tocar `hosts`, omite la llamada desde Order y usa la prueba directa
con `curl --resolve` de más abajo.

### 11.2 Iniciar Inventory con TLS mutuo

```bash
cd modulo-06-seguridad-enterprise/inventory-service-quarkus
mvn quarkus:dev -Dquarkus.profile=mtls
```

El perfil publica HTTPS en `8444`, carga `inventory-service.p12`, confía en la
CA del workshop y exige certificado cliente.

Prueba el handshake directamente desde macOS/Linux/Git Bash:

```bash
curl -i --resolve inventory-service:8444:127.0.0.1 \
  --cacert certs/ca.crt \
  --cert certs/order-service-client.crt \
  --key certs/order-service-client.key \
  -H 'X-Client-Id: order-service-mtls-client' \
  'https://inventory-service:8444/internal/v1/tenants/plataforma/inventory/ZAP-RUN-42?region=LATAM'
```

Ejecuta ese comando desde `modulo-06-seguridad-enterprise`. En Windows,
`curl.exe` admite las mismas opciones si OpenSSL puede leer los archivos PEM.

### 11.3 Iniciar Order con llamada real y Vault

**macOS / Linux / Git Bash**

```bash
cd modulo-06-seguridad-enterprise/order-service-spring
VAULT_ENABLED=true \
INVENTORY_SIMULATE_CALL=false \
INVENTORY_INTERNAL_BASE_URL=https://inventory-service:8444 \
mvn spring-boot:run
```

**Windows PowerShell**

```powershell
cd modulo-06-seguridad-enterprise/order-service-spring
$env:VAULT_ENABLED = 'true'
$env:INVENTORY_SIMULATE_CALL = 'false'
$env:INVENTORY_INTERNAL_BASE_URL = 'https://inventory-service:8444'
mvn spring-boot:run
```

Repite el endpoint de Carla. Debe devolver `previewMode: false` y el body real
de Inventory. Para aislar fallos:

- sin certificado cliente, el handshake de `8444` falla;
- con certificado pero sin `X-Client-Id`, Inventory responde `403`;
- el valor local por defecto `local-order-service` tampoco coincide con
  `order-service-mtls-client`; por eso este paso habilita Vault.

> Alcance real de esta demo: `%mtls.quarkus.http.insecure-requests=enabled`
> mantiene `8084` activo y la ruta `/internal/*` tiene policy `permit`. Por
> tanto, `8444` demuestra mTLS real, pero la configuración incluida **no**
> garantiza que el endpoint interno solo sea alcanzable por mTLS. En producción
> deshabilita HTTP o separa el listener/red y deriva la identidad del
> certificado, no de un header.

## 12. Código clave

Order Service:

- [`SecurityConfig.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/security/SecurityConfig.java):
  autentica todas las APIs y habilita seguridad por método.
- [`RealmRoleConverter.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/security/RealmRoleConverter.java):
  convierte scopes y roles de Keycloak en authorities Spring.
- [`OrderController.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/api/OrderController.java):
  combina RBAC y ABAC.
- [`TenantClaimAuthorizer.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/security/TenantClaimAuthorizer.java)
  y [`RegionClaimAuthorizer.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/security/RegionClaimAuthorizer.java):
  comparan claims y permiten bypass a `orders_admin`.
- [`InventoryMtlsClient.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/client/InventoryMtlsClient.java):
  construye el cliente TLS y el preview.
- [`VaultSecretClient.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/security/order/client/VaultSecretClient.java):
  lee KV v2 por HTTP.

Inventory Service:

- [`InventoryResource.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/security/inventory/api/InventoryResource.java):
  usa `@RolesAllowed`.
- [`AccessPolicyService.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/security/inventory/security/AccessPolicyService.java):
  aplica ABAC a tenant y región.
- [`InternalInventoryResource.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/security/inventory/api/InternalInventoryResource.java)
  e [`InternalClientPolicyService.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/security/inventory/security/InternalClientPolicyService.java):
  implementan el endpoint interno y su comprobación didáctica.

Configuración:

- [`application.yml`](order-service-spring/src/main/resources/application.yml):
  issuer, Vault, preview y material TLS.
- [`application.properties`](inventory-service-quarkus/src/main/resources/application.properties):
  OIDC, rutas y perfil mTLS.
- [`docker-compose.yml`](docker-compose/docker-compose.yml): Keycloak y Vault.

## 13. Restaurar y detener

Detén ambos procesos Maven con `Ctrl+C`. Limpia las variables si las exportaste.

**macOS / Linux / Git Bash**

```bash
unset VAULT_ENABLED INVENTORY_SIMULATE_CALL INVENTORY_INTERNAL_BASE_URL
```

**Windows PowerShell**

```powershell
Remove-Item Env:VAULT_ENABLED -ErrorAction SilentlyContinue
Remove-Item Env:INVENTORY_SIMULATE_CALL -ErrorAction SilentlyContinue
Remove-Item Env:INVENTORY_INTERNAL_BASE_URL -ErrorAction SilentlyContinue
```

Elimina manualmente la línea `inventory-service` de `hosts` si la agregaste.
Después detén la infraestructura.

```bash
cd modulo-06-seguridad-enterprise/docker-compose
docker compose down
```

Con Podman usa `podman compose down`. `down` elimina los contenedores y, como
Vault es efímero, la próxima ejecución requiere volver a sembrar el secreto.

## Solución de problemas

### Keycloak todavía no responde

Revisa `docker compose logs keycloak`. La primera importación tarda más que el
arranque del contenedor. Si modificaste el realm desde la consola y necesitas
volver al estado original, ejecuta `docker compose down` y luego
`docker compose up -d`.

### El token es `null` o recibo 401

- Revisa usuario, contraseña, realm y `client_id=student-portal`.
- Solicita un token nuevo: expira en 300 segundos.
- Comprueba que `iss` sea
  `http://localhost:8180/realms/joedayz-microservices`.
- En PowerShell usa `$AccessToken`; en Bash usa `$ACCESS_TOKEN`.

### Recibo 403 con un token válido

Comprueba primero el rol y después `tenant_id`/`region`. Ana no escribe; Bruno
solo opera en `tienda-deportes/PE`; el endpoint mTLS exige admin.

### Order no inicia

Keycloak debe estar disponible porque Spring resuelve el issuer OIDC al
arrancar. También confirma que `8086` esté libre.

### Inventory no inicia o no publica 8444

El puerto `8444` solo existe con `-Dquarkus.profile=mtls`. Regenera
certificados y ejecuta Maven desde `inventory-service-quarkus`, porque las
rutas de keystore son relativas a ese directorio.

### La llamada real falla por TLS

- Usa `https://inventory-service:8444`, no `https://localhost:8444`.
- Confirma la entrada en `hosts`.
- Verifica que ambos servicios leen los archivos de `certs/` recién generados.
- No uses `-k`: ocultaría errores de confianza/hostname que el ejercicio busca
  detectar.

### Vault responde 404 o el preview falla

Vault dev perdió los datos. Repite la siembra y comprueba exactamente
`secret/data/module6/inventory-client`. El script de política no se aplica
automáticamente.

## Lecturas del módulo

- [01 · Keycloak como Identity Provider](docs/01-keycloak-identity-provider.md)
- [02 · OAuth2 y OpenID Connect desde cero](docs/02-oauth2-openid-connect-desde-cero.md)
- [03 · JWT: firma, validación y refresh](docs/03-jwt-firma-validacion-refresh-tokens.md)
- [04 · Spring Security Resource Server](docs/04-spring-security-resource-server.md)
- [05 · Quarkus OIDC](docs/05-quarkus-oidc.md)
- [06 · mTLS, RBAC y ABAC](docs/06-mtls-rbac-abac.md)
- [07 · HashiCorp Vault](docs/07-hashicorp-vault-secretos.md)
- [08 · Guía didáctica](docs/08-guia-paso-a-paso.md)
