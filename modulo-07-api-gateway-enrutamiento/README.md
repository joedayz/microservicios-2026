# Módulo 7 – API Gateway y Enrutamiento

> Curso: **Arquitectura de Microservicios Pro: Spring Boot + Quarkus en AWS y Azure**  
> JoeDayz.pe · Java 21 / Spring Boot 4 / Spring Cloud Gateway / Quarkus 3 / Kong / AWS API Gateway / Azure API Management

En este módulo la plataforma gana una **puerta de entrada única** para exponer y proteger los
microservicios. El foco práctico está en **Spring Cloud Gateway** como gateway ejecutable local,
mientras **Kong Gateway**, **AWS API Gateway** y **Azure API Management** quedan modelados como
equivalentes operacionales y de nube.

## Objetivos de aprendizaje

Al terminar este módulo serás capaz de:

1. Implementar **routing** centralizado hacia microservicios downstream.
2. Aplicar **filters** globales y por ruta con **Spring Cloud Gateway**.
3. Configurar **rate limiting**, **retries**, **timeouts** y **circuit breaker**.
4. Explicar **hedging** como estrategia de tráfico para requests sensibles a latencia.
5. Proteger el borde con **CORS**, **throttling** y un **WAF básico**.
6. Comparar cuándo usar **Kong Gateway**, **AWS API Gateway** y **Azure API Management**.

## Proyectos del módulo

| Proyecto | Rol | Stack | Puerto |
|----------|-----|-------|--------|
| [api-gateway-service](api-gateway-service/) | Edge gateway, routing, CORS, throttling, retries, circuit breaker, WAF básico | Spring Boot 4 + Spring Cloud Gateway + Resilience4j + Redis | **8080** |
| [inventory-service-quarkus](inventory-service-quarkus/) | Backend downstream para tráfico de inventario | Quarkus 3 + OIDC | **8084** / **8444** |

> El gateway enruta hacia el `order-service-spring` del **módulo 6** en `8086` y hacia el
> `inventory-service-quarkus` de este módulo en `8084`.

## Mapa del módulo

```mermaid
flowchart LR
    CLIENT["SPA / Mobile / Partner"]
    GW["Spring Cloud Gateway<br/>:8080<br/>CORS · Rate Limit · Retry · CB · WAF"]
    ORD["Order Service<br/>:8086"]
    INV["Inventory Service<br/>:8084"]
    REDIS["Redis<br/>Rate limiter state"]
    CLOUD["AWS API Gateway / Azure APIM"]
    KONG["Kong Gateway"]

    CLIENT --> GW
    GW --> ORD
    GW --> INV
    GW --> REDIS
    KONG -. patrones equivalentes .-> GW
    CLOUD -. políticas gestionadas .-> GW
```

## Infraestructura local

El módulo reutiliza **Keycloak** y **Vault** del módulo 6, y agrega **Redis** para throttling del
gateway.

### 1) Levantar Keycloak y Vault

```bash
cd modulo-07-api-gateway-enrutamiento/docker-compose
docker compose up -d keycloak vault
```

### 2) Levantar Redis

```bash
docker run -d --name modulo7-redis -p 6379:6379 redis:7-alpine
```

## Cómo ejecutar los servicios

1. Backend de pedidos del módulo 6:

```bash
cd modulo-06-seguridad-enterprise/order-service-spring
mvn spring-boot:run
```

2. Backend de inventario del módulo 7:

```bash
cd modulo-07-api-gateway-enrutamiento/inventory-service-quarkus
mvn quarkus:dev
```

3. API Gateway:

```bash
cd modulo-07-api-gateway-enrutamiento/api-gateway-service
mvn spring-boot:run
```

## Endpoints expuestos por el gateway

- `GET /gateway/orders/api/v1/tenants/{tenantId}/orders`
- `POST /gateway/orders/api/v1/tenants/{tenantId}/orders`
- `GET /gateway/inventory/api/v1/tenants/{tenantId}/inventory`
- `GET /gateway/inventory/api/v1/tenants/{tenantId}/inventory/{sku}?region=PE`
- `POST /gateway/inventory/api/v1/tenants/{tenantId}/inventory/{sku}/reserve`
- `GET /gateway/admin/traffic-policies`

Probar las políticas de tráfico del gateway (no requiere token):

```bash
curl -i http://localhost:8080/gateway/admin/traffic-policies
```

## Qué demuestra cada capacidad

### Spring Cloud Gateway

- `application.yml`: rutas, filtros, CORS, retry, circuit breaker, rate limiter.
- `filter/CorrelationIdGatewayFilter.java`: propaga o genera `X-Correlation-Id`.
- `filter/BasicWafFilter.java`: bloquea user agents y patrones básicos de path/query.
- `filter/HedgingTrafficFilter.java`: ilustra una estrategia de **hedging** controlada por header.
- `api/FallbackController.java`: respuestas de degradación cuando abre el circuit breaker.

### Gestión del tráfico

- **Retries**: activos para `GET` con backoff exponencial.
- **Timeouts**: configurados por backend vía `Resilience4j TimeLimiter`.
- **Rate limiting / throttling**: `RequestRateLimiter` con Redis por tenant.
- **Hedging**: se activa enviando `X-Hedge-Request: true`.

### Kong / AWS / Azure

`GET /gateway/admin/traffic-policies` resume el mapeo conceptual:

- **Kong**: plugins `rate-limiting`, `cors`, `proxy-cache`, `bot-detection`.
- **AWS API Gateway**: usage plans, throttling, authorizers y AWS WAF.
- **Azure API Management**: policies, `rate-limit-by-key`, JWT validation y multi-region gateway.

## Smoke test rápido

Obtener un token de Keycloak para `bruno-manager` (tenant `tienda-deportes`, región `PE`):

```bash
ACCESS_TOKEN=$(curl --fail --silent --show-error \
  -X POST 'http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=student-portal' \
  -d 'username=bruno-manager' \
  -d 'password=secret123' | jq -r '.access_token')

test -n "$ACCESS_TOKEN" && test "$ACCESS_TOKEN" != "null" && echo "Token obtenido"
```

El comando requiere `jq`. Con `ACCESS_TOKEN` definido en la misma terminal:

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE'
```

Forzar hedging:

```bash
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'X-Hedge-Request: true' \
  'http://localhost:8080/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE'
```

Disparar WAF básico:

```bash
curl -i -H 'User-Agent: sqlmap/1.8' \
  http://localhost:8080/gateway/orders/api/v1/tenants/tienda-deportes/orders
```

## Siguiente referencia

El stack queda listo para extenderlo con una guía didáctica, manifests de Kong y ejemplos de
policies para AWS/Azure sin tocar la base del gateway local.
