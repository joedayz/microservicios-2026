# 04 · Health checks y probes de Kubernetes

## Las tres probes que importan

Kubernetes distingue tres tipos de health check con **semánticas distintas**:

| Probe | Qué evalúa | Qué pasa si falla | Cuándo se ejecuta |
|-------|------------|-------------------|-------------------|
| **startupProbe** | ¿La app terminó de arrancar? | Kubelet **espera** (no ejecuta liveness/readiness). Si excede `failureThreshold`, mata el pod. | Solo al inicio. Se apaga cuando pasa 1 vez. |
| **livenessProbe** | ¿El proceso está vivo? | Kubelet **reinicia** el contenedor. | Continuamente, tras startup. |
| **readinessProbe** | ¿Puedo recibir tráfico ahora? | Kubelet quita el pod del Service (endpoint). **No reinicia**. | Continuamente. |

**Regla de oro**: liveness reinicia, readiness enruta. Nunca los mezcles.

## Qué incluir en cada probe

### livenessProbe

Solo lo mínimo indispensable para saber que el proceso sigue vivo:

- Endpoint HTTP que devuelva 200 sin tocar dependencias externas.
- Ejemplo: `/actuator/health/liveness` (Spring), `/q/health/live` (Quarkus).

**Antipatrón**: incluir la conexión a la BD. Un blip de red en la BD reinicia todos los pods a la vez → **outage total**.

### readinessProbe

Sí puede validar dependencias, **pero solo las críticas**:

- Migrations aplicadas.
- Conexión a la BD principal disponible.
- Estado del Circuit Breaker de un downstream **crítico**.

Si tu CB está `OPEN`, quizás quieras marcar `NOT_READY` para que Kubernetes deje de mandarte tráfico y lo redirija a otros pods (asumiendo que los otros pods no comparten el problema).

### startupProbe

Java/JVM apps a menudo necesitan 30-60 s de warmup. Sin `startupProbe`, tendrías que poner `initialDelaySeconds: 60` en liveness (retrasando la detección de fallos reales). Con `startupProbe`:

```yaml
startupProbe:
  httpGet: { path: /actuator/health/liveness, port: http }
  failureThreshold: 30
  periodSeconds: 2
```

Da 60 s de gracia al arranque, luego `livenessProbe` con `periodSeconds: 10` reacciona rápido a fallos reales.

## Health groups en Spring Boot

Spring Boot 4 permite definir **grupos** de health indicators:

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState,diskSpace
        readiness:
          include: readinessState,db,downstreamCircuitBreakers
```

Cada grupo se expone en `/actuator/health/{group}` y devuelve `UP` solo si **todos** los indicators del grupo lo están.

## Health checks en Quarkus (SmallRye Health)

Quarkus implementa MicroProfile Health con tres anotaciones:

```java
@Liveness   // /q/health/live
@Readiness  // /q/health/ready
@Startup    // /q/health/started
```

Cada bean anotado implementa `HealthCheck` y devuelve `HealthCheckResponse`. Ejemplo:

```java
@Readiness
@ApplicationScoped
public class PricingReadiness implements HealthCheck {
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("pricing")
            .status(pricingIsUp())
            .withData("latencyMs", lastLatency)
            .build();
    }
}
```

## Configuración recomendada de probes

```yaml
startupProbe:
  failureThreshold: 30
  periodSeconds: 2          # total: 60 s de arranque
livenessProbe:
  initialDelaySeconds: 20
  periodSeconds: 10
  failureThreshold: 3       # tolera ~30 s de inestabilidad
readinessProbe:
  initialDelaySeconds: 5
  periodSeconds: 5
  failureThreshold: 2       # reacciona en ~10 s
```

## Integración Circuit Breaker + Readiness

El patrón implementado en el módulo:

```
readiness = livenessState + estado del CB
```

- Si el CB está `CLOSED` o `HALF_OPEN` → `UP` (aún puedo procesar).
- Si el CB está `OPEN` → `OUT_OF_SERVICE` (sáquenme del Service).

Esto tiene un matiz importante: si **todos** los pods tienen el CB abierto, todos serán marcados not-ready y el Service se queda sin endpoints. Kubernetes devolvería 503 a los clientes. Para evitar esto, algunos equipos prefieren **no** ligar readiness al CB y dejar que los fallbacks respondan.

**Decisión de arquitectura**: ¿prefieres 503 rápido (fail-fast) o fallbacks degradados? El módulo demuestra el ligado a CB porque es más didáctico, pero es una decisión consciente.

## Verificación local

```bash
# Spring Boot
curl -s http://localhost:8087/actuator/health/liveness  | jq
curl -s http://localhost:8087/actuator/health/readiness | jq

# Quarkus
curl -s http://localhost:8085/q/health/live    | jq
curl -s http://localhost:8085/q/health/ready   | jq
curl -s http://localhost:8085/q/health/started | jq
```

## Siguiente lectura

→ [05-quarkus-smallrye-fault-tolerance.md](05-quarkus-smallrye-fault-tolerance.md)

