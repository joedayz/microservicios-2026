# Módulo 7 – Workshop de API Gateway y enrutamiento

> **Spring Cloud Gateway · Redis Rate Limiter · Resilience4j · Kong**
>
> Java 21 · Spring Boot 4 · Quarkus 3 · Keycloak

Este workshop continúa la aplicación de los módulos anteriores. Los clientes
dejan de llamar directamente a Order e Inventory: entran por Spring Cloud
Gateway, que autentica el JWT, enruta, limita tráfico, agrega headers y aplica
retry, circuit breaker, CORS y un WAF didáctico.

```text
Cliente
  └─ Spring Cloud Gateway :8080
       ├─ order-service del módulo 6 :8086
       └─ inventory-service del módulo 7 :8084

Keycloak :8180 ─ JWT
Redis :6379 ─ contadores del rate limiter
Kong :8000 / Admin API :8001 ─ alternativa local opcional
```

Al finalizar habrás:

1. levantado la misma identidad de Keycloak usada en el módulo 6;
2. iniciado Redis, Order, Inventory y el gateway;
3. recorrido las rutas y filtros desde sus interfaces web;
4. probado autenticación, routing, headers y CORS;
5. provocado throttling, bloqueo WAF y fallback;
6. distinguido el hedging real de la simulación incluida;
7. comparado Spring Cloud Gateway con Kong;
8. identificado las rutas opcionales para AWS y Azure.

## 0. Prerrequisitos

Necesitas:

- JDK 21 o superior y Maven 3.9 o superior;
- Docker Desktop, Docker Engine o Podman;
- macOS/Linux/Git Bash: `curl` y `jq`;
- Windows: PowerShell 7+;
- puertos libres `6379`, `8080`, `8084`, `8086`, `8180` y `8200`;
- para Kong opcional: puertos `8000` y `8001`.

Ejecuta los comandos desde la raíz de `microservicios-2026`, salvo que un paso
indique otra terminal.

### Comprobar herramientas

**macOS / Linux / Git Bash**

```bash
java -version
mvn -version
docker version
docker compose version
curl --version
jq --version
```

**Windows PowerShell**

```powershell
java -version
mvn -version
docker version
docker compose version
curl.exe --version
$PSVersionTable.PSVersion
```

Con Podman sustituye `docker` por `podman` y `docker compose` por
`podman compose`.

## 1. Levantar Keycloak y Vault

El compose del módulo reutiliza el realm, usuarios y roles del módulo 6.
Abre la **Terminal 1**.

**Docker — Windows, macOS o Linux**

```bash
cd modulo-07-api-gateway-enrutamiento/docker-compose
docker compose up -d
docker compose ps
```

**Podman — macOS o Linux**

```bash
cd modulo-07-api-gateway-enrutamiento/docker-compose
podman compose up -d
podman compose ps
```

Comprueba Keycloak:

**macOS / Linux / Git Bash**

```bash
curl -fsS http://localhost:8180/realms/joedayz-microservices | jq '.realm'
```

**Windows PowerShell**

```powershell
(Invoke-RestMethod `
  http://localhost:8180/realms/joedayz-microservices).realm
```

El resultado debe ser `joedayz-microservices`. Vault mantiene la continuidad
del módulo 6, pero el flujo básico de este gateway no lee secretos de Vault.

### Recorrido web de Keycloak

Abre [http://localhost:8180/admin](http://localhost:8180/admin).

- usuario: `admin`;
- contraseña: `admin`;
- realm: **joedayz-microservices**.

Revisa **Clients → student-portal**, **Realm roles** y el usuario
`bruno-manager`. Sus claims `tenant_id=tienda-deportes` y `region=PE` serán
validados de nuevo por los backends.

## 2. Levantar Redis

Redis no está incluido en el compose anterior. Inícialo por separado:

**Docker**

```bash
docker run -d --name modulo7-redis -p 6379:6379 redis:7-alpine
docker exec modulo7-redis redis-cli ping
```

**Podman**

```bash
podman run -d --name modulo7-redis -p 6379:6379 redis:7-alpine
podman exec modulo7-redis redis-cli ping
```

La respuesta esperada es `PONG`. Spring Cloud Gateway guarda aquí los tokens
del rate limiter; sin Redis el proceso puede arrancar, pero las rutas limitadas
fallarán.

## 3. Levantar Order Service

Abre la **Terminal 2** y déjala ejecutándose:

```bash
cd modulo-06-seguridad-enterprise/order-service-spring
mvn spring-boot:run
```

Comprueba `http://localhost:8086/actuator/health`. Debe mostrar `UP`.

Este es el mismo Order protegido del módulo 6. No se duplica dentro del módulo
7 porque la aplicación del curso evoluciona de un módulo al siguiente.

## 4. Levantar Inventory Service

Abre la **Terminal 3** y déjala ejecutándose:

```bash
cd modulo-07-api-gateway-enrutamiento/inventory-service-quarkus
mvn quarkus:dev
```

Comprueba:

**macOS / Linux / Git Bash**

```bash
curl -fsS http://localhost:8084/q/health/ready | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8084/q/health/ready |
  ConvertTo-Json -Depth 5
```

## 5. Levantar Spring Cloud Gateway

Abre la **Terminal 4** y déjala ejecutándose:

```bash
cd modulo-07-api-gateway-enrutamiento/api-gateway-service
mvn spring-boot:run
```

Verifica:

**macOS / Linux / Git Bash**

```bash
curl -fsS http://localhost:8080/actuator/health | jq
curl -fsS http://localhost:8080/gateway/admin/traffic-policies | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health |
  ConvertTo-Json -Depth 5
Invoke-RestMethod http://localhost:8080/gateway/admin/traffic-policies |
  ConvertTo-Json -Depth 8
```

El endpoint administrativo es público para fines didácticos.

### Workshop web del gateway

Abre en el navegador:

- [Health](http://localhost:8080/actuator/health)
- [Rutas cargadas](http://localhost:8080/actuator/gateway/routes)
- [Políticas didácticas](http://localhost:8080/gateway/admin/traffic-policies)

En las rutas confirma:

- `/gateway/orders/**` → `http://localhost:8086`;
- `/gateway/inventory/**` → `http://localhost:8084`;
- `StripPrefix=2`;
- `CircuitBreaker` y `RequestRateLimiter`.

El endpoint de políticas resume intenciones del laboratorio. La configuración
efectiva siempre es
[`application.yml`](api-gateway-service/src/main/resources/application.yml).
Por ejemplo, ese JSON menciona `request-termination` para Kong, pero
[`kong.yml`](kong/kong.yml) no habilita actualmente dicho plugin.

## 6. Obtener un token

Todos los usuarios demo usan `secret123`. Trabajaremos con `bruno-manager`.

**macOS / Linux / Git Bash**

```bash
ACCESS_TOKEN=$(curl -fsS -X POST \
  http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d grant_type=password \
  -d client_id=student-portal \
  -d username=bruno-manager \
  -d password=secret123 | jq -r '.access_token')

test -n "$ACCESS_TOKEN" && test "$ACCESS_TOKEN" != null && echo "Token obtenido"
```

**Windows PowerShell**

```powershell
$TokenResponse = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body @{
    grant_type='password'; client_id='student-portal'
    username='bruno-manager'; password='secret123'
  }
$AccessToken = $TokenResponse.access_token
$Auth = @{ Authorization = "Bearer $AccessToken" }
```

El password grant es solo para el laboratorio. En producción usa Authorization
Code + PKCE.

## 7. Probar autenticación y routing

### Sin token

```bash
curl -i \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

En PowerShell:

```powershell
try {
  Invoke-WebRequest `
    http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

El gateway responde `401`.

### Order a través del gateway

**macOS / Linux / Git Bash**

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

**Windows PowerShell**

```powershell
Invoke-WebRequest `
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders `
  -Headers $Auth
```

### Inventory a través del gateway

**macOS / Linux / Git Bash**

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE'
```

**Windows PowerShell**

```powershell
Invoke-WebRequest `
  'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE' `
  -Headers $Auth
```

Ambas solicitudes pasan primero la autenticación del gateway y luego la
autorización RBAC/ABAC del backend. El gateway no sustituye esa segunda capa.

## 8. Observar headers transversales

Envía una correlación conocida:

**macOS / Linux / Git Bash**

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'X-Correlation-Id: workshop-m07-001' \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

**Windows PowerShell**

```powershell
$Headers = @{
  Authorization = "Bearer $AccessToken"
  'X-Correlation-Id' = 'workshop-m07-001'
}
Invoke-WebRequest `
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders `
  -Headers $Headers
```

Busca en la respuesta:

- `X-Correlation-Id: workshop-m07-001`;
- `X-Gateway-Region: LATAM`.

Si omites la correlación, el filtro genera un UUID. El gateway también agrega
`X-Forwarded-By: spring-cloud-gateway` hacia el backend.

## 9. Probar el WAF básico

El WAF se ejecuta antes de la autenticación. Simula una herramienta bloqueada:

**macOS / Linux / Git Bash**

```bash
curl -i -H 'User-Agent: sqlmap/1.8' \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

**Windows PowerShell**

```powershell
$Response = Invoke-WebRequest `
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders `
  -UserAgent 'sqlmap/1.8' -SkipHttpErrorCheck
$Response.StatusCode
$Response.Headers['X-WAF-Action']
```

Resultado esperado: `403` y `X-WAF-Action: blocked`.

Esto es un filtro educativo por patrones, no reemplaza AWS WAF, Azure WAF,
ModSecurity ni un servicio de protección administrado.

## 10. Probar throttling con Redis

Los límites configurados son:

- Order: 5 solicitudes por segundo, ráfaga máxima 10;
- Inventory: 10 solicitudes por segundo, ráfaga máxima 20.

Lanza 25 solicitudes concurrentes a Order.

**macOS / Linux / Git Bash**

```bash
seq 1 25 | xargs -P 25 -I {} curl -sS -o /dev/null -w '%{http_code}\n' \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

**Windows PowerShell 7+**

```powershell
1..25 | ForEach-Object -Parallel {
  $Response = Invoke-WebRequest `
    http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders `
    -Headers @{ Authorization = "Bearer $using:AccessToken" } `
    -SkipHttpErrorCheck
  $Response.StatusCode
} -ThrottleLimit 25
```

Debes observar respuestas `200` y `429 Too Many Requests`. El key resolver usa
`X-Tenant-ID` si está presente; de lo contrario extrae el tenant de
`/tenants/{tenantId}` y finalmente usa la IP. El JWT se valida, pero el rate
limiter no compara ese key con el claim `tenant_id`. Esta simplificación sirve
para el laboratorio; en producción deriva el tenant de la identidad validada.

Inspecciona Redis:

```bash
docker exec modulo7-redis redis-cli --scan
```

Con Podman cambia `docker` por `podman`.

## 11. Entender el “hedging” incluido

Prueba el header:

**macOS / Linux / Git Bash**

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'X-Hedge-Request: true' \
  'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE'
```

**Windows PowerShell**

```powershell
Invoke-WebRequest `
  'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE' `
  -Headers @{
    Authorization = "Bearer $AccessToken"
    'X-Hedge-Request' = 'true'
  }
```

La respuesta incluye `X-Hedging-Strategy: synthetic-parallel-request`.

> El nombre del header es intencionalmente didáctico: la implementación actual
> espera 250 ms y realiza **una sola** llamada downstream. No dispara dos
> solicitudes en paralelo ni cancela la perdedora. Un hedging real requiere
> dos suscripciones controladas, idempotencia y cancelación segura.

Revisa
[`HedgingTrafficFilter.java`](api-gateway-service/src/main/java/pe/joedayz/microservicios/security/order/filter/HedgingTrafficFilter.java)
para comprobarlo.

## 12. Retry, circuit breaker y fallback

Los retries solo aplican a `GET`: dos reintentos con backoff. Detén Inventory
con `Ctrl+C` en la Terminal 3 y realiza varias consultas.

**macOS / Linux / Git Bash**

```bash
for i in $(seq 1 6); do
  curl -sS -i -H "Authorization: Bearer $ACCESS_TOKEN" \
    'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE'
done
```

**Windows PowerShell**

```powershell
1..6 | ForEach-Object {
  Invoke-WebRequest `
    'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE' `
    -Headers $Auth -SkipHttpErrorCheck
}
```

El gateway termina devolviendo el fallback `503` con:

```json
{
  "service": "inventory-service",
  "status": "degraded",
  "message": "Circuit breaker abierto: responde el fallback del gateway."
}
```

Consulta métricas y rutas:

- [métricas](http://localhost:8080/actuator/metrics)
- [rutas](http://localhost:8080/actuator/gateway/routes)

Reinicia Inventory repitiendo el paso 4 y espera a que readiness vuelva a
`UP`. El circuit breaker espera 10 segundos antes de permitir recuperación.

## 13. Kong Gateway opcional

Kong es una alternativa local, no un segundo filtro dentro de Spring Gateway.
No tiene GUI en este compose: se inspecciona por su **Admin API**.

Desde `modulo-07-api-gateway-enrutamiento/kong`:

**macOS / Linux / Git Bash**

```bash
./demo.sh up
```

**Windows PowerShell**

```powershell
.\demo.ps1 up
```

También puedes usar directamente:

```bash
docker compose up -d
```

Abre:

- [estado del nodo](http://localhost:8001/status)
- [servicios](http://localhost:8001/services)
- [rutas](http://localhost:8001/routes)
- [plugins](http://localhost:8001/plugins)

Prueba el proxy conservando el JWT:

**macOS / Linux / Git Bash**

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8000/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

**Windows PowerShell**

```powershell
Invoke-WebRequest `
  http://localhost:8000/gateway/orders/api/v1/tenants/tienda-deportes/orders `
  -Headers $Auth
```

La configuración activa routing, CORS, rate limiting, correlación,
`bot-detection` y caché de GET para Inventory.

> El plugin JWT de Kong está comentado porque requiere cargar la clave pública
> real de Keycloak. Sin token, Kong puede enrutar igualmente, pero el backend
> responderá `401` o `403`. No esperes un `200` anónimo ni atribuyas esa
> protección a Kong. Tampoco está activo `request-termination`.

Para apagar Kong:

```bash
./demo.sh down
```

En PowerShell usa `.\demo.ps1 down`.

## 14. AWS y Azure (tracks opcionales)

No necesitas una cuenta cloud para completar este workshop local.

- [`aws-api-gateway/DEPLOY.md`](aws-api-gateway/DEPLOY.md) explica SAM, Usage
  Plans, throttling, Lambda authorizer, VPC Link y WAFv2.
- [`azure-apim/DEPLOY.md`](azure-apim/DEPLOY.md) explica Bicep, APIM,
  `validate-jwt`, `rate-limit-by-key`, retry y caché.

Estos tracks crean recursos facturables y requieren credenciales, región,
subscription y limpieza explícita. Ejecútalos solo si tu clase ha asignado una
cuenta y un presupuesto. No son prerrequisito para los módulos siguientes.

## 15. Restaurar y apagar

1. Detén Gateway, Inventory y Order con `Ctrl+C`.
2. Elimina Redis.

**Docker**

```bash
docker rm -f modulo7-redis
```

**Podman**

```bash
podman rm -f modulo7-redis
```

3. Detén Keycloak y Vault.

```bash
cd modulo-07-api-gateway-enrutamiento/docker-compose
docker compose down
```

Con Podman usa `podman compose down`. Si levantaste Kong, ejecuta también su
comando `down`.

## Solución de problemas

### El gateway no inicia

- comprueba `8080`;
- confirma Keycloak en `8180`, porque Spring resuelve el issuer al arrancar;
- confirma que el JDK sea compatible con el proyecto.

### Todas las rutas devuelven 401

- solicita un token nuevo; los tokens demo expiran en 300 segundos;
- conserva `Bearer ` antes del JWT;
- verifica que el issuer sea el realm local.

### Las rutas devuelven 500 o no aplican rate limit

Redis no está incluido en Compose. Confirma `redis-cli ping`, el puerto `6379`
y el contenedor `modulo7-redis`.

### Order o Inventory devuelven 403

El JWT es válido, pero el backend rechazó RBAC/ABAC. Bruno solo opera en
`tienda-deportes` y región `PE`.

### El fallback no desaparece al reiniciar el backend

Espera al menos 10 segundos para que el circuit breaker salga de `OPEN` y
vuelve a probar.

### Kong responde 502

- confirma que Order e Inventory están activos en el host;
- revisa `docker compose logs kong`;
- en Linux confirma el mapping `host.docker.internal:host-gateway`;
- no inicies Kong antes que sus backends si quieres ejecutar el smoke test.

### Kong devuelve 401 o 403 sin token

Es la respuesta del backend protegido. El plugin JWT de Kong no está activo en
la configuración incluida.

### No encuentro Grafana o Prometheus

Este módulo no los incluye. La observabilidad con Prometheus y Grafana comienza
en el módulo 8 y se amplía con OpenTelemetry, Tempo y Loki en el módulo 9.

## Archivos clave

- [`application.yml`](api-gateway-service/src/main/resources/application.yml):
  rutas, filtros, CORS, límites, retries, breakers y timeouts.
- [`SecurityConfig.java`](api-gateway-service/src/main/java/pe/joedayz/microservicios/security/order/security/SecurityConfig.java):
  autenticación JWT en el borde.
- [`CorrelationIdGatewayFilter.java`](api-gateway-service/src/main/java/pe/joedayz/microservicios/security/order/filter/CorrelationIdGatewayFilter.java):
  correlación transversal.
- [`BasicWafFilter.java`](api-gateway-service/src/main/java/pe/joedayz/microservicios/security/order/filter/BasicWafFilter.java):
  bloqueo didáctico por patrones.
- [`HedgingTrafficFilter.java`](api-gateway-service/src/main/java/pe/joedayz/microservicios/security/order/filter/HedgingTrafficFilter.java):
  retardo sintético, timeout y header de demostración.
- [`FallbackController.java`](api-gateway-service/src/main/java/pe/joedayz/microservicios/security/order/api/FallbackController.java):
  respuestas degradadas.
- [`kong.yml`](kong/kong.yml): configuración declarativa DB-less.
- [`kong/DEPLOY.md`](kong/DEPLOY.md): ejecución y diagnóstico de Kong.

## Siguiente módulo

El [módulo 8](../modulo-08-resiliencia/) profundiza Retry, TimeLimiter,
Circuit Breaker, Bulkhead, fallback y health checks.
