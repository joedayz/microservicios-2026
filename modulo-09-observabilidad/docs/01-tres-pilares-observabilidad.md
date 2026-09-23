# 01 · Los tres pilares, sobre el mismo checkout

La plataforma de este curso es una sola: el e-commerce multi-tenant del módulo 1.
Este módulo no inventa otro sistema. Observa el flujo que ya existe.

```text
POST /api/v1/orders          order-service-spring      :8087   Spring Boot 4
  ├─ reserva stock           inventory-service-quarkus :8085   Quarkus 3
  │    └─ precio             flaky-downstream          :8090   pricing
  └─ riesgo (en paralelo)    flaky-downstream          :8090   risk
```

Los puertos, el SKU `ZAP-RUN-42`, el Circuit Breaker y los fallbacks son los del módulo 8.
El `tenant_id` es el del módulo 1 (`tienda-deportes`, `libreria-lima`, `moda-boutique`).
En los módulos 6 y 7 ese valor viaja dentro del JWT; aquí entra por el header
`X-Tenant-Id` para poder verlo en una traza sin levantar Keycloak.

## Qué es observabilidad

Observabilidad es poder explicar un fallo **sin desplegar código nuevo**. Con logs sueltos
eso no alcanza: un 500 en order no dice si se cayó inventory, si abrió el circuit breaker
o si pricing devolvió el precio cacheado.

Los tres pilares responden preguntas distintas:

| Pilar | Pregunta | En este módulo |
|-------|----------|----------------|
| **Métricas** | ¿Cuánto y qué tan rápido? | Prometheus scrapea Micrometer |
| **Trazas** | ¿Por dónde pasó esta orden? | OpenTelemetry → Tempo |
| **Logs** | ¿Qué decidió el código en ese momento? | OpenTelemetry → Loki |

Grafana es la ventana. No es un cuarto pilar: es donde los tres se cruzan.

## RED, no solo "está arriba"

Para el checkout medimos:

- **Rate**: órdenes por segundo (`http_server_requests` y `orders_placed_total`).
- **Errors**: 5xx y respuestas degradadas (`degradation != NONE`).
- **Duration**: p95 de `POST /api/v1/orders`.

Un fallback es una respuesta **degradada**, no un éxito pleno. El módulo 8 ya separaba
`CONFIRMED` de `PENDING`. Aquí esa decisión queda en una métrica de negocio y en un
atributo de la traza, para no mezclarla con los 200 OK.

## La regla del tenant

El módulo 1 fijó una invariante: toda query, todo evento y todo caché lleva `tenant_id`.
La observabilidad hereda la misma regla.

| Dónde | `tenant.id` | Por qué |
|-------|-------------|---------|
| Span (Tempo) | sí, atributo | alta cardinalidad, una orden = un tenant |
| Log (Loki) | sí, MDC | filtrar "todo lo de tienda-deportes" |
| Métrica (Prometheus) | no | miles de tenants revientan la serie temporal |

`status` y `degradation` sí son labels: el conjunto es chico (`CONFIRMED`, `PENDING`,
`NONE`, `INVENTORY_FALLBACK`, `RISK_FALLBACK`, `BOTH_FALLBACK`).

## Qué vas a ver al final de la clase

Una sola traza, con un solo `traceId`, que entra por order, se parte en dos ramas
(stock y riesgo) y en la rama de stock sigue hacia pricing. Si abres el circuit breaker,
la rama desaparece y queda el log del fallback. El mismo `traceId` está en la consola,
en el JSON de la orden y en Loki.

## Siguiente lectura

[02 · OpenTelemetry: modelo y propagación](02-opentelemetry-modelo-y-propagacion.md)
