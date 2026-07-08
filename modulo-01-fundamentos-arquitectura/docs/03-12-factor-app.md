# 3. The Twelve-Factor App

La metodología **12-Factor** (Adam Wiggins / Heroku, 2011) define cómo construir aplicaciones
**cloud-native**: portables, escalables horizontalmente y aptas para entrega continua. Es la
lista de verificación mínima antes de decir "mi servicio está listo para Kubernetes".

| # | Factor | Qué significa | Cómo se ve en Spring Boot / Quarkus |
|---|--------|---------------|-------------------------------------|
| 1 | **Codebase** | Un repo por app, muchos deploys | 1 repo por microservicio, misma imagen a dev/stage/prod |
| 2 | **Dependencies** | Declaradas y aisladas explícitamente | `pom.xml` / `build.gradle`; nada de libs "del sistema" |
| 3 | **Config** | En el entorno, no en el código | Variables de entorno / ConfigMap / Secret, no `application.yml` hardcodeado |
| 4 | **Backing services** | Recursos adjuntos e intercambiables | La BD, Kafka, Redis se configuran por URL; cambiar de local a RDS = cambiar env |
| 5 | **Build, release, run** | Etapas separadas y estrictas | `mvn package` → imagen inmutable + config → `run` |
| 6 | **Processes** | Procesos **stateless**, share-nothing | No guardes sesión en memoria; usa Redis/JWT |
| 7 | **Port binding** | La app expone su propio puerto | Tomcat/Netty embebido; nada de servidor externo |
| 8 | **Concurrency** | Escala por procesos (scale-out) | Réplicas en Kubernetes (HPA), no threads gigantes |
| 9 | **Disposability** | Arranque rápido y apagado graceful | Startup veloz (¡Quarkus/GraalVM!), maneja `SIGTERM` |
| 10 | **Dev/prod parity** | Entornos lo más parecidos posible | Docker en local = Docker en prod; misma versión de BD |
| 11 | **Logs** | Como stream de eventos a stdout | Log a `stdout`; Loki/CloudWatch los recolectan (no archivos locales) |
| 12 | **Admin processes** | Tareas de admin como procesos one-off | Migraciones Flyway como Job de K8s, no a mano |

### Vista general: los 12 factores en el ciclo de vida

```mermaid
flowchart LR
    subgraph DEV["Desarrollo"]
        F1["1 Codebase"]
        F2["2 Dependencies"]
        F13["13 API First"]
    end

    subgraph BUILD["Build & Release"]
        F5["5 Build/Release/Run"]
        F3["3 Config"]
        F4["4 Backing Services"]
    end

    subgraph RUN["Ejecución en Kubernetes"]
        F6["6 Stateless"]
        F7["7 Port Binding"]
        F8["8 Concurrency"]
        F9["9 Disposability"]
        F10["10 Dev/Prod Parity"]
        F11["11 Logs → stdout"]
        F12["12 Admin Jobs"]
    end

    DEV --> BUILD --> RUN
```

---

## Los factores que más duelen si los ignoras

### Factor 3 — Config en el entorno

**La misma imagen** de contenedor debe correr en dev, staging y prod. Lo único que cambia es la
config, que llega por variables de entorno. Nunca hornees credenciales en la imagen.

```yaml
# application.yml — valores por defecto, sobrescribibles por entorno
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/shop}
    username: ${DB_USER:shop}
    password: ${DB_PASSWORD}     # sin default: obliga a inyectarlo (Secret/Vault)
```

> En el curso, los secretos irán a **HashiCorp Vault** (Módulo 6) y como **Secrets** en
> Kubernetes (Módulo 11).

```mermaid
flowchart TB
    subgraph IMAGEN["🐳 Una sola imagen Docker"]
        APP["order-service:1.2.0<br/>(inmutable)"]
    end

    subgraph ENTORNOS["Config por entorno (Factor 3)"]
        DEV["dev<br/>DB_URL=localhost<br/>DB_PASSWORD=dev"]
        STG["staging<br/>DB_URL=rds-stg<br/>DB_PASSWORD=secret-stg"]
        PRD["prod<br/>DB_URL=rds-prod<br/>DB_PASSWORD=secret-prod"]
    end

    APP --> DEV
    APP --> STG
    APP --> PRD

    VAULT["HashiCorp Vault<br/>(Módulo 6)"] -.->|"inyecta secretos"| PRD
    K8S["K8s Secrets / ConfigMap<br/>(Módulo 11)"] -.-> ENTORNOS
```

### Factor 6 — Procesos stateless

Si guardas el carrito o la sesión en memoria del proceso, no puedes escalar a N réplicas ni
reiniciar sin perder datos. El estado va a un **backing service** (PostgreSQL, Redis) o al
cliente (JWT). Esto habilita el **Factor 8 (Concurrency)** y el **9 (Disposability)**.

```mermaid
flowchart TB
    subgraph MAL["❌ Stateful — no escala bien"]
        LB1["Load Balancer"]
        P1["Pod 1<br/>sesión en RAM"]
        P2["Pod 2<br/>sesión en RAM"]
        LB1 --> P1
        LB1 --> P2
        NOTE1["Usuario A va al Pod 2 → sesión perdida ❌"]
    end

    subgraph BIEN["✅ Stateless — N réplicas iguales"]
        LB2["Load Balancer"]
        P3["Pod 1"]
        P4["Pod 2"]
        P5["Pod 3"]
        REDIS[("Redis / JWT<br/>estado externo")]
        LB2 --> P3
        LB2 --> P4
        LB2 --> P5
        P3 --> REDIS
        P4 --> REDIS
        P5 --> REDIS
    end
```

### Factor 9 — Disposability (¡ventaja de Quarkus!)

Kubernetes crea y destruye pods constantemente (autoscaling, rolling updates, spot nodes). Tu
servicio debe:
- **Arrancar rápido**: aquí Quarkus + GraalVM native brillan (arranque en decenas de ms vs
  segundos de un Spring Boot en JVM). Lo mediremos en el Módulo 2.
- **Apagar graceful**: al recibir `SIGTERM`, dejar de aceptar tráfico, terminar los requests
  en curso y cerrar conexiones. Spring Boot lo soporta con `server.shutdown=graceful`.

```mermaid
sequenceDiagram
    participant K8s as Kubernetes
    participant Pod as Microservicio
    participant LB as Load Balancer

    K8s->>Pod: SIGTERM (rolling update)
    Pod->>LB: deja de aceptar tráfico nuevo
    Pod->>Pod: termina requests en curso
    Pod->>Pod: cierra conexiones BD/Kafka
    Pod->>K8s: exit 0 ✅

    Note over Pod: Quarkus native: arranque ~50ms<br/>Spring Boot JVM: arranque ~2-5s<br/>(lo medimos en Módulo 2)
```

### Factor 11 — Logs a stdout

En cloud-native **no escribes a archivos**. Emites logs estructurados (JSON) a `stdout` y la
plataforma los recolecta (Loki/CloudWatch/Log Analytics — Módulos 9, 13, 14). Esto permite
correlacionar logs con trazas (OpenTelemetry).

```mermaid
flowchart LR
    APP["Microservicio"] -->|"JSON a stdout"| COL["Agente / Promtail"]
    COL --> LOKI["Loki / CloudWatch"]
    LOKI --> GRAF["Grafana"]
    OTEL["OpenTelemetry<br/>(trace_id)"] -.->|"correlación"| GRAF

    note1["❌ No escribir a /var/log/app.log"]
```

---

## Factores extendidos (15-Factor, Kevin Hoffman)

Complementos modernos muy relevantes para microservicios:

- **API first**: diseña el contrato (OpenAPI/proto) antes que la implementación (Módulo 3).
- **Telemetry**: métricas, trazas y logs como ciudadanos de primera clase (Módulo 9).
- **Authentication & Authorization**: seguridad desde el día uno (Módulo 6).

---

## Checklist rápido para tu microservicio

- [ ] ¿La misma imagen corre en todos los entornos con solo cambiar env vars?
- [ ] ¿No hay ningún secreto en el repositorio ni en la imagen?
- [ ] ¿El proceso es stateless (puedo correr 5 réplicas)?
- [ ] ¿Arranca en < 2s y maneja `SIGTERM`?
- [ ] ¿Loguea a stdout en JSON?
- [ ] ¿Las migraciones de BD corren como job, no manualmente?

---

## Ejercicios

1. Encuentra en un proyecto viejo tuyo una violación del Factor 3 y arréglala con env vars.
2. Explica por qué un servicio con sesión en memoria rompe los factores 6, 8 y 9 a la vez.
3. Escribe el `application.yml` de un servicio de pedidos cumpliendo los factores 3 y 4.
