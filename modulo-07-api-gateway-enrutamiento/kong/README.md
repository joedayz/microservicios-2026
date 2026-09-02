# Kong Gateway – Módulo 7

Ejemplo de **Kong Gateway** en modo DB-less que replica las políticas del
`api-gateway-service` (Spring Cloud Gateway) para comparar ambos productos.

## Arrancar

```bash
cd modulo-07-api-gateway-enrutamiento/kong
docker compose up -d
```

- Proxy: <http://localhost:8000>
- Admin API: <http://localhost:8001>

## Rutas expuestas

| Ruta Kong | Backend | Plugins |
|-----------|---------|---------|
| `GET/POST /gateway/orders/**` | `host.docker.internal:8086` (order-service) | `rate-limiting` (60 rpm por `X-Tenant-ID`), `request-transformer`, `bot-detection` |
| `GET/POST /gateway/inventory/**` | `host.docker.internal:8084` (inventory-service) | `rate-limiting` (120 rpm), `proxy-cache` (10s), `bot-detection` |

Plugins globales: `cors`, `correlation-id`, `jwt`.

## Probar

```bash
curl -i http://localhost:8000/gateway/inventory/api/v1/tenants/tienda-deportes/inventory \
  -H 'X-Tenant-ID: tienda-deportes'

# Bot detection - devuelve 403
curl -i http://localhost:8000/gateway/orders/api/v1/tenants/tienda-deportes/orders \
  -H 'User-Agent: sqlmap/1.8'
```

## Equivalencias con Spring Cloud Gateway

| Spring Cloud Gateway (yml) | Kong plugin |
|----------------------------|-------------|
| `RequestRateLimiter` (Redis) | `rate-limiting` |
| `CircuitBreaker` + `TimeLimiter` | `retries` + `read_timeout` + health-checks |
| `Retry` filter | `retries` en el service |
| `globalcors` | `cors` |
| `BasicWafFilter` | `bot-detection`, `ip-restriction` |
| `CorrelationIdGatewayFilter` | `correlation-id` |
| `oauth2ResourceServer` | `jwt` plugin |

## Nota sobre JWT

El bloque `jwt_secrets` incluye un placeholder. Para validarlo contra el
Keycloak del módulo 6/7, exporta la clave pública del realm y reemplázala:

```bash
curl -s http://localhost:8180/realms/joedayz-microservices \
  | jq -r '.public_key' \
  | sed 's/.\{64\}/&\n/g'
```
