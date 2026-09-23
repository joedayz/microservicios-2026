# 07 · Observabilidad de la resiliencia

Los patrones de resiliencia sin observabilidad son **ilusiones de robustez**: crees estar protegido hasta que en producción descubres que el CB nunca abrió, o abrió y no te enteraste. Este doc cubre qué medir, cómo alertar y qué dashboards construir.

## Las 4 métricas doradas de la resiliencia

| Métrica | Qué señala | PromQL |
|---------|-----------|--------|
| **CB state** | ¿Algún downstream está aislado? | `resilience4j_circuitbreaker_state{state="open"} == 1` |
| **Failure rate** | Tendencia del downstream | `rate(resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}[5m])` |
| **Fallback rate** | % de respuestas degradadas | `rate(orders_fallback_total[5m]) / rate(orders_total[5m])` |
| **Bulkhead saturation** | ¿Concurrencia al límite? | `resilience4j_bulkhead_available_concurrent_calls == 0` |

## Métricas por stack

### Spring Boot 4 (Resilience4j + Micrometer)

Endpoint: `/actuator/prometheus`

```
resilience4j_circuitbreaker_state{name,state}
resilience4j_circuitbreaker_calls_seconds{name,kind,quantile}
resilience4j_circuitbreaker_slow_calls{name,kind}
resilience4j_circuitbreaker_not_permitted_calls_total{name}
resilience4j_retry_calls_total{name,kind}
resilience4j_bulkhead_available_concurrent_calls{name}
resilience4j_timelimiter_calls_total{name,kind}
```

### Quarkus (SmallRye FT + Micrometer)

Endpoint: `/q/metrics`

```
ft_circuitbreaker_state_total{method,state}
ft_circuitbreaker_calls_total{method,circuitBreakerResult}
ft_retry_calls_total{method,retried,retryResult}
ft_timeout_calls_total{method,timedOut}
ft_bulkhead_calls_total{method,bulkheadResult}
```

## Correlación con logs

Cada evento del CB debería aparecer en logs con `X-Correlation-Id`. En Spring Boot:

```java
@Bean
RegistryEventConsumer<CircuitBreaker> cbLogger() {
    return event -> event.onEvent(e -> {
        log.info("[cb] name={} type={} details={}",
            e.getCircuitBreakerName(), e.getEventType(), e);
    });
}
```

Así en Loki/ELK puedes buscar `type=STATE_TRANSITION AND name=inventoryClient` y ver el historial completo.

## Dashboard Grafana mínimo

3 paneles esenciales:

1. **Circuit Breakers - Current State** (Stat)
   ```
   sum by (name, state) (resilience4j_circuitbreaker_state == 1)
   ```
2. **Failure rate por CB** (Timeseries)
   ```
   sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}[1m]))
   /
   sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_count[1m]))
   ```
3. **Bulkhead available slots** (Timeseries)
   ```
   resilience4j_bulkhead_available_concurrent_calls
   ```

En clase, con el docker-compose de este módulo, Prometheus scrapea automáticamente los 3 servicios y Grafana está en `http://localhost:3000`.

## Alertas recomendadas

```yaml
# prometheus/alerts.yml
groups:
  - name: resiliencia
    rules:
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 2m
        annotations:
          summary: "Circuit breaker {{ $labels.name }} lleva 2min abierto"

      - alert: HighFallbackRate
        expr: |
          sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}[5m]))
          / sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_count[5m])) > 0.3
        for: 5m
        annotations:
          summary: "{{ $labels.name }} degradado >30% durante 5min"

      - alert: BulkheadSaturated
        expr: resilience4j_bulkhead_available_concurrent_calls == 0
        for: 1m
        annotations:
          summary: "Bulkhead {{ $labels.name }} saturado"
```

## SLO / SLI alrededor de fallbacks

Un fallback es una respuesta **degradada**, no un éxito pleno. Sepáralos en tus SLIs:

```
success_rate = ok / total
degraded_rate = fallback / total
error_rate = 5xx / total

SLO: success_rate + degraded_rate ≥ 99.9%
```

Así el CB **contribuye** a tu SLO en vez de romperlo.

## Chaos engineering complementario

Este módulo introduce chaos "manual" con el `flaky-downstream-service`. En producción, herramientas como:

- **Chaos Monkey for Spring Boot** (`spring-boot-chaos-monkey`): inyecta latencia y excepciones a nivel de beans anotados.
- **Toxiproxy**: proxy TCP que inyecta latencia, corta conexiones, corrompe payloads. Ideal para tests de integración.
- **LitmusChaos / Chaos Mesh** (Kubernetes): kill pods, network partitions, IO stress.

Ejecuta chaos **con** métricas activas para validar que:

- El CB abre cuando debe.
- El fallback responde con la latencia esperada.
- Los dashboards muestran el evento.
- Las alertas disparan.

## Cierre del módulo

Con estas 7 lecturas + los 3 proyectos ejecutables (Spring Boot 4, Quarkus 3, flaky) tienes:

- La teoría de cada patrón.
- Implementación en ambos stacks.
- Health checks correctos para Kubernetes.
- Instrumentación con Prometheus.
- Scripts para reproducir escenarios de fallo.

El siguiente paso es el [módulo 9](../../modulo-09-observabilidad/): la misma orden, de punta a punta, en Tempo y Loki (order → inventory → pricing/risk), con el `traceId` y el `tenant.id` juntos.

