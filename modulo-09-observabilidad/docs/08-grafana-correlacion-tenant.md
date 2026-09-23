# 08 · Grafana: de la métrica a la traza y al log

Grafana entra en `http://localhost:3000` (admin / admin). El acceso anónimo
queda como Editor para poder usar Explore sin loguearte. Hay tres datasources
provisionados y un dashboard, **E-commerce · checkout observable**, en la
carpeta Modulo 09.

## El salto

Los datasources están cableados entre sí:

| Desde | Hacia | Cómo |
|-------|-------|------|
| Prometheus | Tempo | exemplar `trace_id` en el histograma |
| Tempo | Loki | `tracesToLogsV2` filtra por trace id, ±1 minuto |
| Loki | Tempo | campo derivado cuando la línea trae `traceId=` |
| Tempo | Prometheus | service map con las métricas del generator |

El camino de la demo, si el exemplar todavía no aparece:

1. Corre `scripts/smoke-observability.sh`.
2. Copia el `traceId` del JSON.
3. Explore → Tempo → pega el id. Tienes que ver order, inventory y flaky.
4. En la traza, **Logs for this span**. La línea `orden colocada` es la misma orden.
5. Vuelve al dashboard. `Circuit breakers OPEN` en 0 y la serie
   `CONFIRMED · NONE` subió.

## La segunda pasada: degradar

Es el mismo chaos del módulo 8, leído en la traza.

```bash
curl -s -X POST http://localhost:8090/flaky/config \
  -H 'Content-Type: application/json' \
  -d '{"failRate":1.0,"latencyMs":50,"mode":"FAIL"}'

curl -s -X POST http://localhost:8087/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-Id: tienda-deportes' \
  -d '{"orderId":"O-degradada","sku":"ZAP-RUN-42","qty":1}'
```

Espera a que el breaker abra (unas 10 llamadas, como en el módulo 8) y coloca
otra orden. En Tempo la rama hacia flaky desaparece. En Loki aparece
`fallback`. En el dashboard, `degradation` deja de ser `NONE`. Restaura con
`mode=OK` y `failRate=0` antes de seguir.

## Checklist de "se ve el sistema"

- [ ] Una orden sana tiene **un** `traceId` en los tres servicios.
- [ ] `tenant.id=tienda-deportes` está en los spans, no en las métricas.
- [ ] El log de order y el span de order comparten ese id.
- [ ] Con el flaky en `FAIL`, la orden responde y la traza muestra el fallback.
- [ ] `resilience4j_circuitbreaker_state{state="open"}` coincide con lo que Tempo muestra.
- [ ] `/actuator` y `/q/health` no dominan el search de Tempo.

## Kubernetes

`k8s/` repite los Deployments del módulo 8 (mismos probes) y solo agrega el
endpoint del collector:

- Spring: `OTEL_TRACES_ENDPOINT`, `OTEL_LOGS_ENDPOINT`
- Quarkus: `OTEL_EXPORTER_OTLP_ENDPOINT` (sin `/v1/traces`)

El stack de Grafana de la clase es el Compose. En un clúster el collector es
un Deployment más, con el mismo `otel-collector.yaml`, y los pods apuntan a
`http://otel-collector:4318`.

## Cierre

El checkout que el módulo 1 dibujó, el módulo 8 hizo resistente y este módulo
hace explicable: una orden, un tenant, un `traceId`, tres servicios.
