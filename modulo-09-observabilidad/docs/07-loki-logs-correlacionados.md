# 07 · Loki: el log de esa orden, no todos los logs

Loki guarda los logs que los servicios exportan por OTLP. No estamos tailing
un archivo. El appender de Logback (Spring) y la extensión de Quarkus mandan
el registro con el `trace_id` del span activo. El collector lo reenvía a
`http://loki:3100/otlp`.

La consola sigue imprimiendo la misma línea, con `traceId`, `spanId` y
`tenant.id`, para cuando estás en el debugger y Grafana todavía no recargó.

## Consultas de la clase

En Grafana → Explore → Loki:

```logql
{service_name="order-service-spring"}
```

La orden que acaba de colocar el script:

```logql
{service_name="order-service-spring"} | trace_id="<traceId>"
```

El tenant, cuando el cuerpo del log lo trae (el MDC también viaja como atributo):

```logql
{service_name=~"order-service-spring|inventory-service-quarkus|flaky-downstream-service"} |= "tienda-deportes"
```

`service_name` es el label que Loki deriva de `service.name`. Si la consulta
vuelve vacía justo después del curl, espera un segundo: el batch del exporter
está en `1s` y el collector agrupa otro segundo.

## Qué línea buscar

| Servicio | Línea | Cuándo |
|----------|-------|--------|
| order | `orden colocada orderId=... status=... degradation=...` | siempre que el POST terminó |
| order | `fallback reserve` / `fallback risk` | el breaker o el timeout cortó la rama |
| inventory | `reserva orderId=... priceSource=...` | la reserva llegó |
| inventory | `fallback price` | pricing no respondió y se usó el precio cacheado |
| flaky | `price sku=` / `risk orderId=` | el downstream sí entró en la traza |

Si order tiene `orden colocada` y flaky no tiene `price`, el fallback de
inventory se activó: pricing no se ejecutó. La traza en Tempo debe contar la
misma historia. Si no coinciden, el `traceId` del log no es el de la traza
que estás mirando.

## Structured metadata

Loki 3 guarda el `trace_id` de OTLP como structured metadata
(`allow_structured_metadata: true` en `loki.yaml`). Por eso el filtro es
`| trace_id="..."`, no un parser JSON del texto. El texto plano de la consola
es para el humano; Loki no lo está leyendo de stdout.

## Siguiente lectura

[08 · Grafana y la demo](08-grafana-correlacion-tenant.md)
