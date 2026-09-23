# 03 · Spring Boot 4: Micrometer, OTLP y el fan-out

`order-service-spring` es el mismo orquestador del módulo 8. Sigue llamando a inventory
y a risk en paralelo, con el mismo Resilience4j. Lo nuevo es que esa llamada deja rastro.

## Dependencias

| Starter | Para qué |
|---------|----------|
| `spring-boot-starter-opentelemetry` | Micrometer Tracing sobre OTel + export OTLP de trazas |
| `spring-boot-starter-restclient` | `RestClient.Builder` con el interceptor de observacion. En Boot 4 ya no viene dentro de `starter-web` |
| `micrometer-registry-prometheus` | el scrape del módulo 8 |
| `opentelemetry-logback-appender-1.0` | logs hacia el `SdkLoggerProvider` |

Spring Boot arma el bean `OpenTelemetry`, pero **no** conecta Logback solo.
`OpenTelemetryAppenderInitializer` llama a `OpenTelemetryAppender.install(...)` al arrancar.
Sin eso, la consola muestra el `traceId` y Loki se queda vacía.

## A dónde exporta

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    baggage:
      remote-fields: tenant.id
      correlation:
        fields: tenant.id
  opentelemetry:
    tracing.export.otlp.endpoint: http://localhost:4318/v1/traces
    logging.export.otlp.endpoint: http://localhost:4318/v1/logs
```

`correlation.fields` copia `tenant.id` al MDC. Por eso el patrón de log es:

```text
21:04:01.120  INFO [order-service-spring,<traceId>,<spanId>,tienda-deportes] ...
```

Dentro de Docker el endpoint cambia a `http://otel-collector:4318/...`
(`OTEL_TRACES_ENDPOINT` y `OTEL_LOGS_ENDPOINT`).

## El RestClient tiene que ser el auto-configurado

`RestClient.builder()` a mano no lleva el interceptor que escribe `traceparent`.
En Boot 4 el builder es un **prototipo**: cada `@Bean` recibe una copia ya customizada
(observación incluida). Encima agregamos el header `X-Tenant-Id` leído del MDC.

## Virtual threads y el contexto

El fan-out de inventory y risk usa `Executors.newVirtualThreadPerTaskExecutor()`,
el mismo recurso que el módulo 1 introdujo para el checkout. Un hilo nuevo **no**
hereda el `ThreadLocal` del trace.

Por eso cada cliente captura un `ContextSnapshot` en el hilo de la petición y lo
restaura dentro del `supplyAsync`:

```java
ContextSnapshot snapshot = snapshots.captureAll();
return CompletableFuture.supplyAsync(() -> {
    try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
        return http.post()...;
    }
}, executor);
```

Si quitas el `setThreadLocals`, las dos ramas nacen con otro `traceId` y Tempo no las une.

## El span de negocio

`OrderService.place` abre una observación `order.place` con `order.id`, `sku` y
`tenant.id` en **alta cardinalidad** (van a la traza, no a Prometheus). El contador
`orders.placed` solo tiene `status` y `degradation`.

El `traceId` vuelve en el JSON y en el header `X-Trace-Id` para que el script de
la clase no tenga que parsear logs.

## Ruido

`ActuatorObservationPredicate` descarta `/actuator/**`. Si no, cada scrape de
Prometheus es una traza y el checkout se pierde.

## Siguiente lectura

[04 · Quarkus](04-quarkus-opentelemetry.md)
