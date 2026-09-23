# 04 · Quarkus 3: OpenTelemetry en inventory

`inventory-service-quarkus` sigue siendo el servicio que reserva stock y pide el
precio al flaky, con SmallRye Fault Tolerance. La extensión `quarkus-opentelemetry`
instrumenta el server y el REST client: el span de `GET /flaky/price/{sku}` queda
colgando del `POST /reserve` sin un interceptor manual.

## Configuración que importa

```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4318
quarkus.otel.exporter.otlp.protocol=http/protobuf
quarkus.otel.traces.enabled=true
quarkus.otel.logs.enabled=true
quarkus.otel.metrics.enabled=false
quarkus.otel.propagators=tracecontext,baggage
quarkus.otel.traces.sampler=parentbased_always_on
quarkus.otel.traces.suppress-non-application-uris=true
```

El endpoint de Quarkus es la **base**, sin `/v1/traces`. Spring Boot pide la ruta
completa. Es la diferencia que más veces rompe la demo.

`metrics.enabled=false` evita un segundo canal de métricas. Prometheus sigue
scrapeando `/q/metrics`, donde ya están los contadores de SmallRye
(`ft_circuitbreaker_*`, `ft_retry_*`).

`suppress-non-application-uris` esconde `/q/health` y `/q/metrics`. Mismo motivo
que el predicado de Actuator en Spring: los probes no son el checkout.

## Logs con el mismo traceId

El formato de consola imprime el MDC que llena la extensión:

```text
21:04:01 INFO [abc.../def... tenant=tienda-deportes] reserva orderId=O-obs-1 ...
```

Con `quarkus.otel.logs.enabled=true` esos registros también salen por OTLP hacia
el collector y terminan en Loki, atados al `trace_id` del span activo.

## El tenant en el REST client

`TenantContextFilter` (JAX-RS) hace tres cosas al entrar la reserva:

1. Lee `X-Tenant-Id` o, si no viene, el baggage `tenant.id`. Si no hay nada, usa `tienda-deportes`.
2. Pone el atributo `tenant.id` en el span y en el MDC.
3. Abre un scope de baggage para que la llamada a pricing lo propague.

`TenantHeaderFactory` reenvía `X-Tenant-Id` al flaky. No toca `traceparent`:
eso lo escribe la instrumentación del REST client. Si el factory reemplaza el mapa
de headers salientes y tira los que ya puso OTel, la traza se corta en inventory.
Por eso copia `outgoing` y solo agrega el header de negocio.

## El fallback se ve en la traza

Si pricing falla y `PricingClient` devuelve `CACHED_DEFAULT`, el span cliente
marca error y el log dice `fallback price`. El padre (la reserva) igual responde
200 con `priceSource=CACHED_DEFAULT`. Esa es la foto que hay que saber leer:
el checkout no se cayó, se degradó. El módulo 8 lo medía con el circuit breaker;
aquí se ve en la misma traza.

## Siguiente lectura

[05 · Prometheus](05-prometheus-metricas-y-exemplars.md)
