# 05 · Prometheus: las métricas que ya teníamos, más el negocio

Prometheus no cambia de rol respecto al módulo 8. Sigue siendo pull. El
`docker-compose/prometheus.yml` scrapea los tres servicios cada 5 segundos:

| Job | Target | Path |
|-----|--------|------|
| `order-service-spring` | `host.docker.internal:8087` | `/actuator/prometheus` |
| `inventory-service-quarkus` | `host.docker.internal:8085` | `/q/metrics` |
| `flaky-downstream-service` | `flaky-downstream:8090` | `/actuator/prometheus` |

Order e inventory corren en tu máquina. Flaky corre en Compose, por eso su target
es el nombre del servicio y no el host. En Podman, si `host.docker.internal` no
resuelve, cámbialo por `host.containers.internal` (la misma nota del módulo 8).

## Qué series importan

Del módulo 8, siguen vivas:

```promql
resilience4j_circuitbreaker_state{state="open"} == 1
rate(resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}[5m])
```

Nuevas de este módulo:

```promql
# tráfico real, sin probes ni scrapes
sum by (job) (rate(http_server_requests_seconds_count{uri!~"/actuator.*|/q/.*"}[1m]))

# p95 por servicio
histogram_quantile(0.95, sum by (job, le) (
  rate(http_server_requests_seconds_bucket{uri!~"/actuator.*|/q/.*"}[1m])
))

# órdenes colocadas, separando la degradación
sum by (status, degradation) (increase(orders_placed_total[5m]))
```

`orders_placed_total` sale del contador `orders.placed`. No lleva `tenant`:
mira [01](01-tres-pilares-observabilidad.md).

## Exemplars

Prometheus arranca con `--enable-feature=exemplar-storage`. Cuando Micrometer
tiene una traza activa, el scrape puede adjuntar un `trace_id` al bucket del
histograma. Grafana usa eso para saltar de un pico de latencia a la traza en Tempo.

Si el exemplar no aparece, la traza sigue existiendo: búscalo por el `traceId`
del JSON. El exemplar es un atajo, no la única puerta.

## Alertas

`alerts.yml` trae dos reglas chicas:

- `CircuitBreakerOpen`: la misma idea del módulo 8, con `for: 1m` para la clase.
- `OrderErrorRate`: 5xx de order por encima del 5% durante 5 minutos.

Un circuit breaker abierto **no** es por sí solo un 5xx: el fallback responde 200
degradado. Por eso las dos alertas conviven. Si solo miras 5xx, el día que pricing
muere y el fallback aguanta, la alerta no suena y el dashboard de `degradation`
es el que cuenta la historia.

## Remote write

Tempo escribe métricas de service graph (`traces_service_graph_*` y
`traces_spanmetrics_*`) hacia Prometheus (`--web.enable-remote-write-receiver`).
No reemplazan a `http_server_requests`: alimentan el mapa de servicios de Grafana.

## Siguiente lectura

[06 · Tempo](06-tempo-trazas-distribuidas.md)
