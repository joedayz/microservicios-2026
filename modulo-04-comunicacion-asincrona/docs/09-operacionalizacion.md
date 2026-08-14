# 9. Operacionalización: Docker, Healthchecks y Despliegue

## Índice

1. [Objetivo](#objetivo)
2. [Stack local (Docker Compose)](#stack-local-docker-compose)
3. [Despliegue en Kubernetes (Kind + Podman)](#despliegue-en-kubernetes-kind--podman)
4. [Verificación de salud](#verificación-de-salud)
5. [Flujo demo](#flujo-demo)
6. [Observabilidad](#observabilidad)
7. [Checklist de despliegue](#checklist-de-despliegue)

---

## Objetivo

Este cierre del módulo resume cómo llevar la solución asíncrona a un entorno operativo sin perder trazabilidad ni consistencia.

La idea es simple:

- levantar Kafka y Schema Registry de forma reproducible,
- validar que los brokers y servicios responden,
- observar el flujo de eventos,
- y preparar el terreno para un despliegue real en Kubernetes o cloud.

---

## Stack local (Docker Compose)

El módulo incluye un stack local mínimo en `docker-compose/docker-compose.yml`:

- Kafka en KRaft
- Schema Registry
- Kafka UI

Arranque:

```bash
cd modulo-04-comunicacion-asincrona/docker-compose
docker compose up -d
```

Verificación rápida:

```bash
docker compose ps
curl http://localhost:8085/subjects
```

Kafka UI queda en `http://localhost:8090`.

---

## Despliegue en Kubernetes (Kind + Podman)

Si en vez de `podman compose` quieres desplegarlo sobre **Kind**, el módulo incluye
`k8s/` y `scripts/` listos para usar — misma lógica de eventos, mismos consumer groups.

### Estructura

```
k8s/
├── kind/
│   ├── config.yaml          # Cluster Kind con port-mappings
│   └── nodeports.yaml       # NodePorts fijos para curls locales
├── kafka.yaml               # Kafka KRaft + Schema Registry + Kafka UI
├── catalog-deployment.yaml
├── inventory-deployment.yaml
└── order-deployment.yaml

scripts/
├── common.sh                # Helpers Podman/Kind compartidos
├── 01-kind-create.sh        # Crea cluster microservicios-m04
├── 02-deploy.sh             # Build → imágenes → load → kubectl apply
├── 03-smoke.sh              # 3 escenarios Saga con curl
└── 04-destroy.sh            # Elimina el cluster
```

### Flujo completo

```bash
cd modulo-04-comunicacion-asincrona

# 1. Crear cluster Kind (con port-mappings para acceso local)
./scripts/01-kind-create.sh

# 2. Build Maven → Podman → kind load → kubectl apply
#    Orden garantizado: Kafka ready ANTES de arrancar los servicios
./scripts/02-deploy.sh

# 3. Smoke test: happy-path + compensación por sin-stock + SKU inexistente
./scripts/03-smoke.sh

# 4. Limpiar todo
./scripts/04-destroy.sh
```

### Puertos expuestos vía Kind extraPortMappings

| Servicio         | Host              | NodePort |
|------------------|-------------------|----------|
| catalog-service  | `localhost:8081`  | 30081    |
| inventory-service| `localhost:8084`  | 30084    |
| order-service    | `localhost:8086`  | 30086    |
| kafka (externo)  | `localhost:9092`  | 30092    |
| kafka-ui         | `localhost:8090`  | 30090    |

> **Nota macOS:** los scripts usan `127.0.0.1` (no `localhost`) para evitar
> que curl resuelva a `::1` cuando Kind/Podman solo escucha en IPv4.

### Comunicación inter-servicio en el cluster

Dentro del cluster los servicios se conectan a Kafka por DNS ClusterIP:

```
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

La lógica no cambia: mismos topics, mismos consumer groups, mismo flujo de Saga.

---

## Verificación de salud

El script `docker-compose/healthcheck.sh` espera a que el broker y el Schema Registry estén listos antes de continuar.

```bash
cd modulo-04-comunicacion-asincrona/docker-compose
./healthcheck.sh
```

Qué valida:

- broker accesible por `localhost:9092`
- Schema Registry respondiendo en `localhost:8085`

En clase esto sirve para mostrar que un sistema distribuido no se “da por levantado” hasta que sus dependencias clave responden.

---

## Flujo demo

Secuencia recomendada (Docker Compose local):

1. levantar infraestructura local,
2. iniciar los microservicios,
3. publicar una orden (happy-path),
4. observar los topics en Kafka UI,
5. forzar un error (SKU sin stock o inexistente),
6. mostrar la compensación Saga.

**Con los scripts de Kind**, el `03-smoke.sh` ejecuta los 3 escenarios automáticamente:

```bash
# Happy-path: SKU-001 con stock 100 → orden COMPLETED
curl -s -X POST http://127.0.0.1:8086/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: demo-tenant' \
  -d '{"customerId":"customer-001","sku":"SKU-001","quantity":2}'

# Compensación: SKU-003 con stock 0 → orden FAILED (stock insuficiente)
curl -s -X POST http://127.0.0.1:8086/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: demo-tenant' \
  -d '{"customerId":"customer-002","sku":"SKU-003","quantity":1}'

# Compensación: SKU inexistente → orden FAILED (SKU no encontrado)
curl -s -X POST http://127.0.0.1:8086/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: demo-tenant' \
  -d '{"customerId":"customer-003","sku":"SKU-NOEXISTE","quantity":1}'
```

Health checks de los servicios:

```bash
curl http://localhost:8086/actuator/health   # order-service
curl http://localhost:8081/actuator/health   # catalog-service
curl http://localhost:8084/actuator/health   # inventory-service
```

Topics que aparecen en Kafka UI tras el demo:

- `reserve-stock-command`
- `stock-reserved`
- `stock-reservation-failed`
- `inventory-updated`

---

## Observabilidad

Lo mínimo que conviene mirar en el módulo:

- **Logs**: correlación por `orderId` y `X-Tenant-ID`.
- **Topics**: retención, particiones y lag.
- **Consumer groups**: rebalances y offsets.
- **Health checks**: readiness y liveness.
- **Errores**: retries y dead-letter topics.

Si un flujo falla, el diagnóstico sigue este orden:

1. infraestructura,
2. broker,
3. schema registry,
4. producer,
5. consumer,
6. compensación.

---

## Checklist de despliegue

- Kafka con persistencia y particiones suficientes.
- Schema Registry alineado con la compatibilidad de esquemas.
- Topics creados con nombres estables.
- Producers idempotentes cuando aplique.
- Consumers con retry y manejo de errores.
- Outbox para evitar dual write.
- Correlation IDs en logs.
- Métricas y alertas para lag y fallos.

Para Kubernetes, el mismo criterio aplica: primero la conectividad del broker, luego los servicios, luego la validación del flujo.

---

## Resumen

Operacionalizar no es “solo desplegar”.

Es garantizar que el flujo de eventos siga siendo visible, recuperable y consistente cuando el entorno cambia.
