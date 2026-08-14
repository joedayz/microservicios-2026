# 9. Operacionalización: Docker, Healthchecks y Despliegue

## Índice

1. [Objetivo](#objetivo)
2. [Stack local](#stack-local)
3. [Verificación de salud](#verificación-de-salud)
4. [Flujo demo](#flujo-demo)
5. [Observabilidad](#observabilidad)
6. [Checklist de despliegue](#checklist-de-despliegue)

---

## Objetivo

Este cierre del módulo resume cómo llevar la solución asíncrona a un entorno operativo sin perder trazabilidad ni consistencia.

La idea es simple:

- levantar Kafka y Schema Registry de forma reproducible,
- validar que los brokers y servicios responden,
- observar el flujo de eventos,
- y preparar el terreno para un despliegue real en Kubernetes o cloud.

---

## Stack local

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

Secuencia recomendada:

1. levantar infraestructura local,
2. iniciar los microservicios,
3. publicar una orden,
4. observar los topics,
5. forzar un error,
6. mostrar la recuperación.

Endpoints útiles:

```bash
curl http://localhost:8086/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8084/actuator/health
```

Para inspección del flujo:

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
