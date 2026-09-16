# 05 · SmallRye Fault Tolerance en Quarkus

## Qué es MicroProfile Fault Tolerance

Un estándar de anotaciones para patrones de resiliencia, con la misma filosofía que MicroProfile Health: **portabilidad entre implementaciones**. Quarkus usa **SmallRye Fault Tolerance** (la implementación de referencia).

Ventaja frente a Resilience4j directo: las mismas anotaciones funcionan en Quarkus, Helidon, Open Liberty, Payara.

## Anotaciones disponibles

| Anotación | Paquete | Propósito |
|-----------|---------|-----------|
| `@Asynchronous` | `org.eclipse.microprofile.faulttolerance` | Ejecuta el método en otro hilo. |
| `@Timeout` | " | Aborta si supera el tiempo. |
| `@Retry` | " | Reintenta con delay y jitter. |
| `@CircuitBreaker` | " | Corta tráfico ante fallos. |
| `@Bulkhead` | " | Limita concurrencia. |
| `@Fallback` | " | Método alternativo si todo falla. |

## Ejemplo canónico

```java
@ApplicationScoped
public class PricingClient {

    @Inject @RestClient PricingRestClient rest;

    @Bulkhead(value = 25, waitingTaskQueue = 10)
    @Timeout(value = 1500, unit = ChronoUnit.MILLIS)
    @CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 10_000,
        successThreshold = 3)
    @Retry(maxRetries = 3, delay = 300, jitter = 100)
    @Fallback(fallbackMethod = "fallbackPrice")
    public Price fetch(String sku) {
        return rest.priceOf(sku);
    }

    Price fallbackPrice(String sku) {
        return Price.cachedOrDefault(sku);
    }
}
```

## Diferencias frente a Resilience4j

| Aspecto | SmallRye FT | Resilience4j |
|---------|-------------|--------------|
| Especificación | MicroProfile (portable) | Librería propia |
| Configuración | Anotaciones + `microprofile-config` properties | YAML por instancia |
| Sliding window | Basada en **conteo** (`requestVolumeThreshold`) | Count o time |
| Slow calls | No en el core (SmallRye añade `@ApplyGuard`) | Sí, `slowCallRateThreshold` |
| Métricas | MicroProfile Metrics (Prometheus vía Micrometer) | Micrometer nativo |

Si el ADR del proyecto prioriza portabilidad → SmallRye. Si prioriza feature richness → Resilience4j.

## Sobrescribir con configuración

Los valores de las anotaciones pueden sobrescribirse en `application.properties`:

```properties
pe.joedayz.microservicios.resilience.inventory.client.PricingClient/fetch/CircuitBreaker/failureRatio=0.6
pe.joedayz.microservicios.resilience.inventory.client.PricingClient/fetch/Retry/maxRetries=5
```

O globalmente:

```properties
CircuitBreaker/failureRatio=0.6
Retry/maxRetries=5
```

## Nombres de Circuit Breaker

SmallRye usa por defecto `<fully-qualified-classname>/<methodName>` como nombre del CB. En el módulo:

```
pe.joedayz.microservicios.resilience.inventory.client.PricingClient/fetch
```

Este es el nombre que usa `CircuitBreakerMaintenance` para consultarlo desde el health check.

## API CircuitBreakerMaintenance

SmallRye añade una API no estándar muy útil:

```java
@Inject CircuitBreakerMaintenance cbm;

CircuitBreakerState state = cbm.currentState(CB_NAME);
cbm.resetAll();
cbm.reset(CB_NAME);
```

El `PricingCircuitBreakerReadiness` del módulo la usa para decidir si el pod está listo.

## Métricas expuestas

Con `quarkus.fault-tolerance.metrics.enabled=true` y `quarkus-micrometer-registry-prometheus`:

- `ft_circuitbreaker_state_total{method,state}`
- `ft_circuitbreaker_calls_total{method,circuitBreakerResult}`
- `ft_retry_calls_total{method,retried,retryResult}`
- `ft_bulkhead_calls_total{method,bulkheadResult}`
- `ft_timeout_calls_total{method,timedOut}`

Se scrapean desde `/q/metrics`.

## Antipatrones específicos de SmallRye

- **`@Asynchronous` sin `@Bulkhead`**: cada llamada crea un hilo virtual del pool default, y sin bulkhead puedes agotarlo.
- **`@Retry` con `abortOn` mal configurado**: si no listas las excepciones no recuperables, harás retry sobre 4xx.
- **Nombres de CB derivados del classname**: si mueves la clase de paquete, "rompes" el nombre del CB en dashboards y health checks. Considera nombrar explícitamente con `@CircuitBreakerName("pricing-client")` (SmallRye extension).

## Siguiente lectura

→ [06-spring-boot4-resilience4j.md](06-spring-boot4-resilience4j.md)

