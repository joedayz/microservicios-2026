# Módulo 8 – Workshop de resiliencia

> **Resilience4j · SmallRye Fault Tolerance · Circuit Breaker · Health Checks**
>
> Java 21 · Spring Boot 4 · Quarkus 3

Este workshop permite repetir la clase completa sin material adicional. Levantarás
una plataforma pequeña, colocarás una orden sana y provocarás fallos controlados
para observar `Retry`, `TimeLimiter`/`Timeout`, `CircuitBreaker`, `Bulkhead` y
`Fallback`.

```text
Cliente
  └─ order-service-spring :8087
       ├─ inventory-service-quarkus :8085
       │    └─ flaky-downstream-service :8090 (pricing)
       └─ flaky-downstream-service :8090 (risk)
```

Order e inventory se ejecutan con Maven en el host. Docker Compose levanta el
downstream caótico, Prometheus y Grafana.

Al finalizar habrás recorrido el ciclo:

```text
CLOSED --fallos--> OPEN --espera--> HALF_OPEN --éxitos de prueba--> CLOSED
```

## 0. Prerrequisitos

Necesitas:

- JDK 21 o superior.
- Maven 3.9 o superior.
- Docker Desktop en Windows/macOS, o Docker/Podman en Linux.
- Puertos libres `3000`, `8085`, `8087`, `8090` y `9090`.
- macOS/Linux/Git Bash: `curl` y `jq`.
- Windows: PowerShell 7 o superior (`pwsh`).

Ejecuta los comandos desde la raíz del repositorio
`microservicios-2026`, salvo que el paso indique otra terminal.

### Comprobar herramientas

**macOS / Linux / Git Bash**

```bash
java -version
mvn -version
docker version
docker compose version
curl --version
jq --version
```

**Windows PowerShell**

```powershell
java -version
mvn -version
docker version
docker compose version
$PSVersionTable.PSVersion
```

Con Podman sustituye `docker compose` por `podman compose`.

## 1. Levantar la infraestructura

Abre la **Terminal 1**.

**Docker, en Windows, macOS o Linux**

```bash
cd modulo-08-resiliencia/docker-compose
docker compose up -d --build
docker compose ps
```

**Podman, en macOS o Linux**

```bash
cd modulo-08-resiliencia/docker-compose
podman compose up -d --build
podman compose ps
```

Debes ver:

- `modulo8-flaky`, publicado en `8090`;
- `modulo8-prometheus`, publicado en `9090`;
- `modulo8-grafana`, publicado en `3000`.

Solo el contenedor flaky tiene healthcheck; `docker compose ps` debe terminar
mostrándolo como `healthy`. Prometheus y Grafana pueden tardar unos segundos.

### Verificar la infraestructura

**macOS / Linux / Git Bash**

```bash
curl -sS http://localhost:8090/actuator/health | jq
curl -sS http://localhost:9090/-/ready
curl -sS http://localhost:3000/api/health | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8090/actuator/health | ConvertTo-Json
Invoke-RestMethod http://localhost:9090/-/ready
Invoke-RestMethod http://localhost:3000/api/health | ConvertTo-Json
```

Resultados esperados: flaky devuelve `"status":"UP"`, Prometheus
`Prometheus Server is Ready` y Grafana `"database":"ok"`.

> En Docker Desktop, Prometheus alcanza al host mediante
> `host.docker.internal`. En Docker Engine para Linux ese nombre puede no
> existir; consulta la solución en [Troubleshooting](#prometheus-muestra-order-o-inventory-en-down).

## 2. Levantar inventory-service-quarkus

Abre la **Terminal 2** y déjala ejecutándose:

```bash
cd modulo-08-resiliencia/inventory-service-quarkus
mvn quarkus:dev
```

Espera al mensaje de arranque y comprueba las tres probes desde otra terminal.

**macOS / Linux / Git Bash**

```bash
curl -sS http://localhost:8085/q/health/live | jq
curl -sS http://localhost:8085/q/health/ready | jq
curl -sS http://localhost:8085/q/health/started | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8085/q/health/live | ConvertTo-Json -Depth 6
Invoke-RestMethod http://localhost:8085/q/health/ready | ConvertTo-Json -Depth 6
Invoke-RestMethod http://localhost:8085/q/health/started | ConvertTo-Json -Depth 6
```

Las tres deben indicar `UP`. El startup check `warmup` permanece `DOWN` durante
los primeros tres segundos. Readiness incluye `pricing-circuit-breaker`.

## 3. Levantar order-service-spring

Abre la **Terminal 3** y déjala ejecutándose:

```bash
cd modulo-08-resiliencia/order-service-spring
mvn spring-boot:run
```

Comprueba las probes.

**macOS / Linux / Git Bash**

```bash
curl -sS http://localhost:8087/actuator/health/liveness | jq
curl -sS http://localhost:8087/actuator/health/readiness | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8087/actuator/health/liveness |
  ConvertTo-Json -Depth 6
Invoke-RestMethod http://localhost:8087/actuator/health/readiness |
  ConvertTo-Json -Depth 6
```

Ambas deben mostrar `UP`. Readiness contiene el componente
`downstreamCircuitBreakers`; liveness no depende de los downstreams.

## 4. Dejar el downstream sano

**macOS / Linux / Git Bash**

```bash
curl -sS -X POST http://localhost:8090/flaky/config \
  -H 'Content-Type: application/json' \
  -d '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post http://localhost:8090/flaky/config `
  -ContentType 'application/json' `
  -Body '{"failRate":0.0,"latencyMs":50,"mode":"OK"}'
```

La respuesta debe indicar `mode: OK`, `failRate: 0` y `latencyMs: 50`.

## 5. Colocar una orden sana

**macOS / Linux / Git Bash**

```bash
curl -sS -X POST http://localhost:8087/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"O-WORKSHOP-001","sku":"ZAP-RUN-42","qty":1}' | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post http://localhost:8087/api/v1/orders `
  -ContentType 'application/json' `
  -Body '{"orderId":"O-WORKSHOP-001","sku":"ZAP-RUN-42","qty":1}' |
  ConvertTo-Json -Depth 8
```

Comprueba:

- `status` es `CONFIRMED`;
- `degradation` es `NONE`;
- `inventory.status` es `RESERVED`;
- `inventory.unitPrice` es `199.90` y `priceSource` es `PRICING_SERVICE`;
- `risk.decision` es `APPROVE` (el `score` es aleatorio).

### Puntos de código

- [`OrderService.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/resilience/order/service/OrderService.java)
  ejecuta inventory y risk en paralelo y calcula `status`/`degradation`.
- [`InventoryClient.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/resilience/order/client/InventoryClient.java)
  aplica Bulkhead, TimeLimiter, CircuitBreaker, Retry y fallback.
- [`RiskClient.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/resilience/order/client/RiskClient.java)
  protege la llamada directa a `/flaky/risk/{orderId}`.
- [`PricingClient.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/resilience/inventory/client/PricingClient.java)
  aplica los equivalentes de SmallRye al pricing.
- [`FlakyController.java`](flaky-downstream-service/src/main/java/pe/joedayz/microservicios/resilience/flaky/FlakyController.java)
  implementa exactamente los modos `OK`, `FAIL`, `SLOW` y `TIMEOUT`.

## 6. Workshop web: Prometheus

Abre [http://localhost:9090](http://localhost:9090).

### 6.1 Comprobar targets

1. Ve a **Status → Targets**.
2. Localiza `order-service-spring`, `inventory-service-quarkus` y
   `flaky-downstream-service`.
3. Confirma `UP` para Order e Inventory.

Flaky solo expone health, no `/actuator/prometheus`; por eso su target puede
mostrar `DOWN` con HTTP 404. Ese es el resultado esperado con la configuración
actual y no impide
los escenarios. Order e inventory sí deben estar `UP`.

### 6.2 Consultas PromQL

En **Graph**, ejecuta una consulta por vez:

```promql
up
```

```promql
sum by (name, state) (resilience4j_circuitbreaker_state == 1)
```

```promql
sum by (name, kind) (increase(resilience4j_circuitbreaker_calls_seconds_count[5m]))
```

```promql
sum by (name, kind) (increase(resilience4j_retry_calls_total[5m]))
```

```promql
resilience4j_bulkhead_available_concurrent_calls
```

```promql
sum by (name, kind) (increase(resilience4j_timelimiter_calls_total[5m]))
```

Para descubrir los nombres que exporta la versión de SmallRye de este checkout:

```promql
{__name__=~"ft_.*"}
```

Las métricas pueden no aparecer hasta que el método protegido haya sido
invocado y Prometheus haya realizado el siguiente scrape (intervalo: 5 s).

## 7. Workshop web: Grafana

Abre [http://localhost:3000](http://localhost:3000).

- Usuario: `admin`
- Contraseña: `admin`
- El acceso anónimo está habilitado con rol Viewer.

Este módulo provisiona el datasource **Prometheus**, no un dashboard. Para no
confundirlo con el dashboard del módulo 9, usa **Explore**:

1. Abre **Explore**.
2. Selecciona el datasource **Prometheus**.
3. Cambia al editor **Code**.
4. Selecciona **Last 15 minutes**.
5. Ejecuta:

```promql
sum by (name, state) (resilience4j_circuitbreaker_state == 1)
```

Luego compara con:

```promql
sum by (name) (rate(resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}[1m]))
```

Y para detectar saturación reciente:

```promql
min_over_time(resilience4j_bulkhead_available_concurrent_calls[5m])
```

Mantén Explore abierto; volverás aquí durante los experimentos.

## 8. Ciclo del Circuit Breaker

El recorrido determinista usa `failRate: 1.0`. Los scripts incluidos usan 70 %
y son útiles como demo probabilística, pero no garantizan la misma secuencia en
cada ejecución.

### 8.1 Confirmar `CLOSED`

**macOS / Linux / Git Bash**

```bash
curl -sS http://localhost:8087/actuator/circuitbreakers | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8087/actuator/circuitbreakers |
  ConvertTo-Json -Depth 8
```

Busca `riskClient` con `state: CLOSED`.

### 8.2 Provocar `OPEN`

Configura fallo total.

**macOS / Linux / Git Bash**

```bash
curl -sS -X POST http://localhost:8090/flaky/config \
  -H 'Content-Type: application/json' \
  -d '{"failRate":1.0,"latencyMs":50,"mode":"FAIL"}' | jq

for i in $(seq 1 12); do
  curl -sS -o /dev/null -X POST http://localhost:8087/api/v1/orders \
    -H 'Content-Type: application/json' \
    -d "{\"orderId\":\"O-FAIL-$i\",\"sku\":\"ZAP-RUN-42\",\"qty\":1}"
done

curl -sS http://localhost:8087/actuator/circuitbreakers | jq
curl -sS http://localhost:8087/actuator/circuitbreakerevents/riskClient | jq
curl -sS http://localhost:8087/actuator/health/readiness | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post http://localhost:8090/flaky/config `
  -ContentType 'application/json' `
  -Body '{"failRate":1.0,"latencyMs":50,"mode":"FAIL"}'

1..12 | ForEach-Object {
  Invoke-RestMethod -Method Post http://localhost:8087/api/v1/orders `
    -ContentType 'application/json' `
    -Body "{`"orderId`":`"O-FAIL-$_`",`"sku`":`"ZAP-RUN-42`",`"qty`":1}" |
    Out-Null
}

Invoke-RestMethod http://localhost:8087/actuator/circuitbreakers |
  ConvertTo-Json -Depth 8
Invoke-RestMethod http://localhost:8087/actuator/circuitbreakerevents/riskClient |
  ConvertTo-Json -Depth 10
try {
  Invoke-RestMethod http://localhost:8087/actuator/health/readiness |
    ConvertTo-Json -Depth 8
} catch {
  $_.ErrorDetails.Message
}
```

Resultados:

- `riskClient` abre tras alcanzar al menos 5 llamadas y 40 % de fallos;
- sus llamadas posteriores son `NOT_PERMITTED` y usan fallback;
- una orden puede seguir respondiendo HTTP 200 con
  `risk.decision: MANUAL_REVIEW` y `degradation: RISK_FALLBACK`;
- order readiness pasa a `OUT_OF_SERVICE`, pero liveness continúa `UP`;
- inventory contiene los errores de pricing con `CACHED_DEFAULT`; su readiness
  pasa a `DOWN` cuando el breaker de pricing está abierto.

En Prometheus o Grafana confirma:

```promql
resilience4j_circuitbreaker_state{name="riskClient",state="open"} == 1
```

### 8.3 Observar `HALF_OPEN`

Restaura flaky y espera más que los 8 s configurados para `riskClient`, sin
enviar órdenes durante la espera.

**macOS / Linux / Git Bash**

```bash
curl -sS -X POST http://localhost:8090/flaky/config \
  -H 'Content-Type: application/json' \
  -d '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' | jq
sleep 9
curl -sS http://localhost:8087/actuator/circuitbreakers | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod -Method Post http://localhost:8090/flaky/config `
  -ContentType 'application/json' `
  -Body '{"failRate":0.0,"latencyMs":50,"mode":"OK"}'
Start-Sleep -Seconds 9
Invoke-RestMethod http://localhost:8087/actuator/circuitbreakers |
  ConvertTo-Json -Depth 8
```

Debes capturar `riskClient` en `HALF_OPEN`. La transición es automática.

### 8.4 Volver a `CLOSED`

`riskClient` permite 3 llamadas de prueba en half-open.

**macOS / Linux / Git Bash**

```bash
for i in $(seq 1 3); do
  curl -sS -o /dev/null -X POST http://localhost:8087/api/v1/orders \
    -H 'Content-Type: application/json' \
    -d "{\"orderId\":\"O-RECOVERY-$i\",\"sku\":\"ZAP-RUN-42\",\"qty\":1}"
done
curl -sS http://localhost:8087/actuator/circuitbreakers | jq
curl -sS http://localhost:8087/actuator/health/readiness | jq
```

**Windows PowerShell**

```powershell
1..3 | ForEach-Object {
  Invoke-RestMethod -Method Post http://localhost:8087/api/v1/orders `
    -ContentType 'application/json' `
    -Body "{`"orderId`":`"O-RECOVERY-$_`",`"sku`":`"ZAP-RUN-42`",`"qty`":1}" |
    Out-Null
}
Invoke-RestMethod http://localhost:8087/actuator/circuitbreakers |
  ConvertTo-Json -Depth 8
Invoke-RestMethod http://localhost:8087/actuator/health/readiness |
  ConvertTo-Json -Depth 8
```

El estado final esperado es `CLOSED` y readiness `UP`. El breaker de pricing de
Quarkus espera 10 s y exige 3 éxitos; si aún figura abierto, espera 2 s más y
coloca otras 3 órdenes.

## 9. Escenario de llamadas lentas

Para activar específicamente el umbral de llamadas lentas de
`inventoryClient`, usa 900 ms en modo `OK`: supera
`slowCallDurationThreshold: 800ms`, pero queda por debajo del TimeLimiter de
1500 ms.

```bash
curl -sS -X POST http://localhost:8090/flaky/config \
  -H 'Content-Type: application/json' \
  -d '{"failRate":0.0,"latencyMs":900,"mode":"OK"}' | jq
```

En PowerShell:

```powershell
Invoke-RestMethod -Method Post http://localhost:8090/flaky/config `
  -ContentType 'application/json' `
  -Body '{"failRate":0.0,"latencyMs":900,"mode":"OK"}'
```

Envía al menos 10 órdenes usando el bucle del paso 8.2 y cambia el prefijo a
`O-SLOW`. El resultado puede seguir siendo funcional, pero
`inventoryClient` abre cuando 60 % de su ventana son llamadas lentas.

Consulta:

```promql
sum by (name, kind) (increase(resilience4j_circuitbreaker_slow_calls[5m]))
```

Restaura `OK/50ms` y completa el ciclo half-open como en el paso 8.

> El escenario `slow` de los scripts configura modo `SLOW`: el controlador
> duerme `latencyMs` y luego cuatro veces más. Con 600 ms suma 3 s, por lo que
> demuestra timeouts, no una llamada lenta exitosa.

## 10. Escenario de timeout

**macOS / Linux / Git Bash**

```bash
./modulo-08-resiliencia/scripts/chaos-scenarios.sh timeout
time curl -sS -X POST http://localhost:8087/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"O-TIMEOUT-001","sku":"ZAP-RUN-42","qty":1}' | jq
```

**Windows PowerShell**

```powershell
.\modulo-08-resiliencia\scripts\chaos-scenarios.ps1 timeout
Measure-Command {
  Invoke-RestMethod -Method Post http://localhost:8087/api/v1/orders `
    -ContentType 'application/json' `
    -Body '{"orderId":"O-TIMEOUT-001","sku":"ZAP-RUN-42","qty":1}' |
    ConvertTo-Json -Depth 8
}
```

Flaky duerme 30 s, pero order tiene TimeLimiters de 1.2 s (risk) y 1.5 s
(inventory). La respuesta llega degradada sin esperar 30 s: risk usa
`MANUAL_REVIEW`; inventory puede usar su fallback `DEGRADED`, por lo que la
orden puede quedar `PENDING/BOTH_FALLBACK`.

Consulta:

```promql
sum by (name, kind) (increase(resilience4j_timelimiter_calls_total[5m]))
```

## 11. Escenario de bulkhead

Con flaky aún en `TIMEOUT`, lanza 50 órdenes concurrentes. Los límites son 25
para inventory y 15 para risk.

**macOS / Linux / Git Bash**

```bash
seq 1 50 | xargs -P 50 -I {} curl -sS -o /dev/null \
  -X POST http://localhost:8087/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"O-BULK-{}","sku":"ZAP-RUN-42","qty":1}'
```

**Windows PowerShell 7+**

```powershell
1..50 | ForEach-Object -Parallel {
  try {
    Invoke-WebRequest -Method Post http://localhost:8087/api/v1/orders `
      -ContentType 'application/json' `
      -Body "{`"orderId`":`"O-BULK-$_`",`"sku`":`"ZAP-RUN-42`",`"qty`":1}" `
      -SkipHttpErrorCheck | Out-Null
  } catch {}
} -ThrottleLimit 50
```

En Grafana ejecuta:

```promql
min_over_time(resilience4j_bulkhead_available_concurrent_calls[5m])
```

Un mínimo de `0` indica saturación. Como el bulkhead es el decorador exterior y
el fallback está declarado en el CircuitBreaker interior, las solicitudes
rechazadas por el bulkhead pueden responder con error en vez de degradarse; las
admitidas terminan por timeout/fallback.

## 12. Scripts incluidos

Desde `modulo-08-resiliencia`:

**macOS / Linux / Git Bash**

```bash
./scripts/chaos-scenarios.sh status
./scripts/chaos-scenarios.sh fail70
./scripts/chaos-scenarios.sh slow
./scripts/chaos-scenarios.sh timeout
./scripts/chaos-scenarios.sh ok
./scripts/smoke-resilience.sh
```

**Windows PowerShell**

```powershell
.\scripts\chaos-scenarios.ps1 status
.\scripts\chaos-scenarios.ps1 fail70
.\scripts\chaos-scenarios.ps1 slow
.\scripts\chaos-scenarios.ps1 timeout
.\scripts\chaos-scenarios.ps1 ok
.\scripts\smoke-resilience.ps1
```

Si PowerShell bloquea scripts:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

`smoke-resilience` restaura flaky, envía 30 órdenes con 70 % de fallo, muestra
los breakers, espera 12 s y envía 10 órdenes sanas. Al ser probabilístico,
repite el script si no abre un breaker.

## 13. Kubernetes opcional

Los manifiestos enseñan la separación correcta de probes:

- order: startup/liveness en `/actuator/health/liveness` y readiness en
  `/actuator/health/readiness`;
- inventory: `/q/health/started`, `/q/health/live` y `/q/health/ready`;
- flaky: `/actuator/health`.

Valida su sintaxis, si tienes `kubectl`:

```bash
kubectl apply --dry-run=client -f modulo-08-resiliencia/k8s/
```

Revisa [`k8s/order-deployment.yaml`](k8s/order-deployment.yaml),
[`k8s/inventory-deployment.yaml`](k8s/inventory-deployment.yaml) y
[`k8s/flaky-deployment.yaml`](k8s/flaky-deployment.yaml).

Los YAML referencian imágenes `modulo8/*:1.0.0`, pero este módulo no incluye un
Dockerfile para inventory ni un flujo completo de carga a un clúster. Por eso
esta sección valida y estudia probes; no afirma que `kubectl apply` despliegue
todo sin preparar las imágenes.

## 14. Restaurar y apagar

Restaura flaky antes de terminar.

**macOS / Linux / Git Bash**

```bash
./modulo-08-resiliencia/scripts/chaos-scenarios.sh ok
```

**Windows PowerShell**

```powershell
.\modulo-08-resiliencia\scripts\chaos-scenarios.ps1 ok
```

Espera 12 s y coloca 3 órdenes sanas si quieres dejar los breakers en
`CLOSED`. Después:

1. Detén order e inventory con `Ctrl+C` en las terminales 3 y 2.
2. Detén los contenedores.

```bash
cd modulo-08-resiliencia/docker-compose
docker compose down
```

Con Podman:

```bash
cd modulo-08-resiliencia/docker-compose
podman compose down
```

Prometheus conserva datos solo dentro del contenedor; al recrearlo empieza sin
el historial anterior.

## Troubleshooting

### Prometheus muestra order o inventory en DOWN

- Confirma primero que los endpoints de métricas responden:
  `http://localhost:8087/actuator/prometheus` y
  `http://localhost:8085/q/metrics`.
- En Docker Desktop comprueba que `host.docker.internal` resuelva.
- En Podman, sustituye ese host por `host.containers.internal` en una copia
  local de [`docker-compose/prometheus.yml`](docker-compose/prometheus.yml).
- En Docker Engine para Linux, el compose actual no declara
  `host-gateway`. Sin editar archivos, obtén el gateway y añádelo al contenedor:

```bash
GATEWAY=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}' modulo8-prometheus)
docker exec -u 0 modulo8-prometheus sh -c "echo '$GATEWAY host.docker.internal' >> /etc/hosts"
```

### El target flaky aparece DOWN en Prometheus

Es esperado: Prometheus intenta `/actuator/prometheus`, pero
`flaky-downstream-service` solo expone `health,info`. Verifica flaky por
`/actuator/health`.

### Readiness devuelve HTTP 503

Es la respuesta esperada cuando un breaker está abierto. En PowerShell usa
`try/catch`; en shell evita `curl -f` si quieres inspeccionar el JSON.
Liveness debe permanecer `UP`.

### El breaker no abre

- Usa `failRate: 1.0` para eliminar azar.
- `riskClient` requiere 5 llamadas; `inventoryClient` requiere 10.
- Pricing de Quarkus contiene sus fallos y devuelve `CACHED_DEFAULT`; por eso
  esos fallos no abren necesariamente el `inventoryClient` de Spring.
- Consulta `/actuator/circuitbreakerevents/riskClient`.

### No logro ver `HALF_OPEN`

No envíes tráfico durante la espera. Consulta el estado justo después de 9 s
para risk o 11 s para inventory. Las llamadas permitidas pueden cerrar el
breaker tan rápido que no alcances a verlo.

### La orden tarda 30 segundos en modo TIMEOUT

Confirma que la petición entra por order (`:8087`), no directamente por flaky,
y que order arrancó con la configuración de
[`application.yml`](order-service-spring/src/main/resources/application.yml).

### Un puerto ya está ocupado

Identifica y detén el proceso anterior. Cambiar solo el puerto publicado de un
servicio rompe las URLs verificadas de este workshop.

## Archivos clave

- [`order/application.yml`](order-service-spring/src/main/resources/application.yml):
  ventanas, umbrales, esperas, timeouts, bulkheads y Actuator.
- [`inventory/application.properties`](inventory-service-quarkus/src/main/resources/application.properties):
  REST client, health y métricas.
- [`docker-compose.yml`](docker-compose/docker-compose.yml): infraestructura.
- [`prometheus.yml`](docker-compose/prometheus.yml): targets y rutas de scrape.
- [`grafana-datasource.yml`](docker-compose/grafana-datasource.yml): datasource
  provisionado.
- [`CircuitBreakerHealthIndicator.java`](order-service-spring/src/main/java/pe/joedayz/microservicios/resilience/order/health/CircuitBreakerHealthIndicator.java):
  readiness de Spring.
- [`PricingCircuitBreakerReadiness.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/resilience/inventory/health/PricingCircuitBreakerReadiness.java):
  readiness de Quarkus.

## Lecturas teóricas

- [01 · Fundamentos de resiliencia](docs/01-fundamentos-resiliencia.md)
- [02 · Resilience4j core](docs/02-resilience4j-core.md)
- [03 · Patrones de Circuit Breaker](docs/03-circuit-breaker-patterns.md)
- [04 · Health checks y Kubernetes](docs/04-health-checks-k8s.md)
- [05 · Quarkus y SmallRye Fault Tolerance](docs/05-quarkus-smallrye-fault-tolerance.md)
- [06 · Spring Boot 4 y Resilience4j](docs/06-spring-boot4-resilience4j.md)
- [07 · Observabilidad de resiliencia](docs/07-observabilidad-resiliencia.md)

## Siguiente módulo

El [módulo 9](../modulo-09-observabilidad/) continúa sobre la misma topología
con OpenTelemetry, Prometheus, Grafana, Tempo y Loki.
