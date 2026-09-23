# 06 · Tempo: una traza para toda la orden

Tempo guarda trazas. No tiene una UI propia en este módulo: se consulta desde
Grafana (Explore → datasource Tempo) o por HTTP:

```text
GET http://localhost:3200/api/traces/<traceId>
```

El collector le entrega OTLP por gRPC en la red de Compose (`tempo:4317`).
Ese puerto no está publicado al host: desde tu máquina solo entra el collector
en `:4318`.

## La forma de una orden sana

`POST /api/v1/orders` con `X-Tenant-Id: tienda-deportes` produce un árbol parecido a este:

```text
order-service-spring
└─ POST /api/v1/orders
   └─ order.place          tenant.id=tienda-deportes  order.id=O-obs-1
      ├─ POST inventory/reserve
      │  └─ inventory-service-quarkus
      │     └─ POST /api/v1/inventory/reserve
      │        └─ GET /flaky/price/{sku}
      │           └─ flaky-downstream-service
      └─ GET /flaky/risk/{orderId}
         └─ flaky-downstream-service
```

Las dos ramas son paralelas (virtual threads). Comparten `traceId` y tienen
`spanId` distintos. Si solo ves una rama, el `ContextSnapshot` no se restauró
en el hilo nuevo: vuelve a [03](03-spring-boot-4-micrometer-otlp.md).

El atributo `tenant.id` tiene que estar en los tres servicios. Si falta en
flaky, el header o el baggage se perdieron en el cliente.

## La forma de una orden degradada

Con el flaky en `FAIL` y el circuit breaker abierto, la rama de pricing o de
risk **no llega** al downstream. En su lugar queda el log `fallback reserve` o
`fallback risk`, y el JSON trae `degradation` distinto de `NONE`.

Eso es lo que hay que mostrar en clase: la traza explica el radio de impacto
que el módulo 8 solo medía con el estado del breaker.

## Service graph

Tempo corre el `metrics-generator` con `service-graphs` y `span-metrics`, y
hace remote write a Prometheus. En el panel de la traza, el Node graph muestra
`order-service-spring → inventory-service-quarkus → flaky-downstream-service`
y la arista directa de risk. Tarda un par de órdenes en poblarse: el generator
agrega por ventana, no por cada span suelto.

## Qué ignorar

Spans de `/actuator` y `/q/health` están filtrados en los servicios. Si aún
aparecen, el predicado o `suppress-non-application-uris` no se aplicó y el
sampler está en always-on para todo.

## Siguiente lectura

[07 · Loki](07-loki-logs-correlacionados.md)
