# 06 · Resilience4j en Spring Boot 4

## Setup mínimo

En el `pom.xml` del `order-service-spring` de este módulo:

```xml
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
  <version>2.3.0</version>
</dependency>
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-micrometer</artifactId>
  <version>2.3.0</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

> Nota: `resilience4j-spring-boot3` funciona con Spring Boot 3.x **y** 4.x, la librería no rompió compatibilidad de auto-configuración.

## Configuración declarativa

Toda la config va en `application.yml`. No se necesita código para instanciar CircuitBreakers, Retries, etc.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventoryClient:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        # ...
  retry:
    instances:
      inventoryClient:
        maxAttempts: 3
        waitDuration: 300ms
```

## Uso con anotaciones

```java
@Component
public class InventoryClient {

    @Bulkhead(name = "inventoryClient", type = Bulkhead.Type.SEMAPHORE)
    @TimeLimiter(name = "inventoryClient")
    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "reserveFallback")
    @Retry(name = "inventoryClient")
    public CompletableFuture<Map<String, Object>> reserve(String sku, int qty, String orderId) {
        return CompletableFuture.supplyAsync(() -> http.post()...);
    }

    private CompletableFuture<Map<String, Object>> reserveFallback(
            String sku, int qty, String orderId, Throwable ex) {
        return CompletableFuture.completedFuture(Map.of("status", "DEGRADED"));
    }
}
```

## Reglas del `fallbackMethod`

1. Misma signatura + `Throwable` (o excepción específica) como último parámetro.
2. Mismo tipo de retorno (o subtipo).
3. Debe estar en la misma clase.
4. Puede haber **varios** fallbacks, cada uno con distinta excepción:
   ```java
   private X fallback(String sku, CallNotPermittedException ex) { ... }
   private X fallback(String sku, TimeoutException ex) { ... }
   private X fallback(String sku, Throwable ex) { ... }
   ```

## TimeLimiter y tipos de retorno

`@TimeLimiter` **requiere** que el método devuelva `CompletableFuture<T>`, `Mono<T>` o `Flux<T>`. No funciona con retornos síncronos. Si tienes un método síncrono, envuélvelo:

```java
public CompletableFuture<X> call() {
    return CompletableFuture.supplyAsync(() -> syncCall());
}
```

## Actuator: endpoints diagnósticos

Con `management.endpoints.web.exposure.include: circuitbreakers,circuitbreakerevents,...`:

| Endpoint | Contenido |
|----------|-----------|
| `/actuator/circuitbreakers` | Estado actual de todos los CBs. |
| `/actuator/circuitbreakerevents` | Últimos 100 eventos globales. |
| `/actuator/circuitbreakerevents/{name}` | Eventos de un CB específico. |
| `/actuator/circuitbreakerevents/{name}/{type}` | Filtrado por tipo (`ERROR`, `SUCCESS`, `STATE_TRANSITION`). |
| `/actuator/retries` | Estado de todos los Retry. |
| `/actuator/retryevents` | Eventos de retry. |
| `/actuator/bulkheads` | Bulkheads activos. |
| `/actuator/timelimiters` | TimeLimiters activos. |

## Health indicator del Circuit Breaker

Spring Boot expone automáticamente un `CircuitBreakersHealthIndicator` si:

```yaml
management:
  health:
    circuitbreakers:
      enabled: true
```

Reporta `UP` si todos los CBs están `CLOSED` o `HALF_OPEN`, `DOWN` si alguno está `OPEN`.

El módulo añade **además** un `CircuitBreakerHealthIndicator` propio que devuelve `OUT_OF_SERVICE` (semánticamente más correcto para readiness): el pod no está roto, simplemente no debe recibir tráfico.

## Integración con RestClient (Spring 6.1+)

En el módulo usamos `RestClient` (sync) envuelto en `CompletableFuture.supplyAsync` para satisfacer `@TimeLimiter`.

**Alternativa reactiva**: si el proyecto es WebFlux, usa `WebClient` + operadores Reactor sin anotaciones:

```java
Mono<Reservation> reserve(String sku) {
    return webClient.post().retrieve().bodyToMono(Reservation.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(RetryOperator.of(retry))
        .transformDeferred(TimeLimiterOperator.of(tl));
}
```

Aquí el orden lo controlas tú explícitamente.

## Sobrescribir por perfil

```yaml
---
spring:
  config:
    activate:
      on-profile: chaos
resilience4j:
  circuitbreaker:
    instances:
      inventoryClient:
        failureRateThreshold: 30
        waitDurationInOpenState: 5s
```

Útil para tests de caos donde quieres ver el CB abrir más rápido.

## Micrometer + Prometheus

Con `resilience4j-micrometer` y `micrometer-registry-prometheus`, las métricas aparecen en `/actuator/prometheus` sin config extra.

Query PromQL útil:

```
resilience4j_circuitbreaker_state{state="open"} == 1
```

Alerta si algún CB queda abierto > 5 min.

## Siguiente lectura

→ [07-observabilidad-resiliencia.md](07-observabilidad-resiliencia.md)

