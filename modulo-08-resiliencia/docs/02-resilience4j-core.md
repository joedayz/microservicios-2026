# 02 · Resilience4j core

**Resilience4j** es la librería de referencia para tolerancia a fallos en el ecosistema Java moderno. Sustituyó a **Hystrix** (Netflix, hoy en mantenimiento) y es la implementación por defecto de Spring Cloud Circuit Breaker.

## Filosofía: library-first, functional

- Cero dependencias externas obligatorias (solo Vavr para operaciones funcionales opcionales, ya removido en 2.x).
- Cada patrón es un **decorator**: recibe una función, devuelve una función decorada.
- Composable: puedes envolver una función con varios decorators y controlar el orden.
- Integración con **Micrometer**, **Reactor**, **Spring Boot** y **CDI**.

## Los 6 módulos principales

| Módulo | Anotación | Propósito |
|--------|-----------|-----------|
| `CircuitBreaker` | `@CircuitBreaker` | Cortar tráfico a un downstream caído. |
| `Retry` | `@Retry` | Reintentar fallos transitorios con backoff. |
| `TimeLimiter` | `@TimeLimiter` | Abortar llamadas que exceden un timeout. |
| `Bulkhead` | `@Bulkhead` | Limitar concurrencia (semáforo o thread pool). |
| `RateLimiter` | `@RateLimiter` | Limitar requests por unidad de tiempo. |
| `Cache` | `@Cache` | Cachear respuestas exitosas. |

## Orden de decoradores recomendado

El orden importa. Con anotaciones Spring Boot, el orden efectivo (de fuera hacia dentro) es:

```
Bulkhead → TimeLimiter → CircuitBreaker → Retry → Fallback
```

Razones:

1. **Bulkhead primero**: si estoy saturado, ni siquiera gasto un hilo abriendo el CB.
2. **TimeLimiter**: aborta la llamada colgada antes de que el CB la registre como lenta.
3. **CircuitBreaker**: registra el resultado (éxito / fallo / lento).
4. **Retry**: si el CB permitió pasar, reintentamos con backoff.
5. **Fallback**: si todo lo anterior falló, respuesta degradada.

Esto se puede cambiar con `spring.aop.proxy-target-class` y `resilience4j.circuitbreaker.aspect-order`, pero el default cubre el 95% de los casos.

## Sliding windows

El Circuit Breaker toma decisiones sobre una **ventana móvil** de resultados recientes:

| Tipo | Cómo cuenta | Cuándo usar |
|------|-------------|-------------|
| `COUNT_BASED` | Últimas N llamadas. | Tráfico estable. |
| `TIME_BASED` | Últimos N segundos. | Tráfico ráfaga (bursty). |

Ejemplo (`application.yml`):

```yaml
resilience4j.circuitbreaker.instances.inventoryClient:
  slidingWindowType: COUNT_BASED
  slidingWindowSize: 20
  minimumNumberOfCalls: 10
  failureRateThreshold: 50
```

Con `minimumNumberOfCalls: 10`, el CB **no abre** hasta que hay al menos 10 llamadas en la ventana. Evita abrir prematuramente durante warmup.

## Slow calls: el CB no solo cuenta errores

Uno de los tuning más útiles:

```yaml
slowCallRateThreshold: 60
slowCallDurationThreshold: 800ms
```

Si el 60% de las llamadas tarda más de 800 ms, el CB abre **aunque no haya errores**. Esto captura downstreams que están "casi vivos" (síntoma clásico previo a caída total).

## Integración con Reactor

Para pipelines `Mono`/`Flux`:

```java
Mono<Reservation> reserve(String sku) {
    return webClient.post().retrieve().bodyToMono(Reservation.class)
        .transformDeferred(RetryOperator.of(retry))
        .transformDeferred(TimeLimiterOperator.of(timeLimiter))
        .transformDeferred(CircuitBreakerOperator.of(cb));
}
```

## Métricas expuestas

Con `resilience4j-micrometer`:

- `resilience4j_circuitbreaker_state{name="inventoryClient",state="open"}` = 1
- `resilience4j_circuitbreaker_calls{name,kind="successful|failed|not_permitted"}`
- `resilience4j_circuitbreaker_slow_calls{name,kind}`
- `resilience4j_retry_calls_total{name,kind}`
- `resilience4j_bulkhead_available_concurrent_calls{name}`

Estas se scrapean automáticamente en `/actuator/prometheus`.

## Eventos en Actuator

Spring Boot expone dos endpoints diagnósticos muy útiles:

- `GET /actuator/circuitbreakers` — estado actual de todos los CB.
- `GET /actuator/circuitbreakerevents/{name}` — últimos eventos (`STATE_TRANSITION`, `NOT_PERMITTED`, `ERROR`, `SUCCESS`).

En clase, ejecutar `curl -s .../circuitbreakerevents/inventoryClient | jq` mientras corre el script de smoke muestra en vivo las transiciones `CLOSED → OPEN → HALF_OPEN → CLOSED`.

## Siguiente lectura

→ [03-circuit-breaker-patterns.md](03-circuit-breaker-patterns.md)

