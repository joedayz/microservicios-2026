# 4. Patrones clave de microservicios

Estos son los patrones que aparecen una y otra vez en arquitecturas reales. Aquí los presentamos
con el problema que resuelven, un diagrama y la referencia al código de demostración.

- [Saga](#saga) — transacciones distribuidas
- [CQRS](#cqrs) — separar lecturas de escrituras
- [Outbox](#transactional-outbox) — publicar eventos de forma fiable
- [API Gateway](#api-gateway) — puerta de entrada única
- [BFF](#bff--backend-for-frontend) — un backend por tipo de cliente
- [Strangler Fig](#strangler-fig) — migrar un legacy sin big-bang

---

## Saga

**Problema:** en un monolito, "crear pedido → cobrar → reservar stock" ocurre en **una
transacción ACID**. En microservicios cada paso vive en un servicio distinto con su propia BD.
No hay transacción distribuida (y las que hay, como 2PC, no escalan). ¿Cómo mantengo consistencia?

**Solución:** una **Saga** es una secuencia de transacciones locales. Si un paso falla, se
ejecutan **transacciones de compensación** que deshacen los pasos anteriores (undo semántico).

Dos sabores:

- **Orquestada:** un coordinador central (`OrderSaga`) dice a cada servicio qué hacer. Más fácil
  de entender, monitorear y debuggear. *Es la que implementamos en el código.*
- **Coreografada:** cada servicio reacciona a eventos de otros, sin coordinador. Menos
  acoplamiento, pero el flujo global es difícil de seguir.

### Orquestada vs coreografada

```mermaid
flowchart TB
    subgraph ORQ["Saga ORQUESTADA (la del código)"]
        SAGA["OrderSaga<br/>(coordinador)"]
        SAGA --> PAY1["Payment"]
        SAGA --> INV1["Inventory"]
        SAGA --> ORD1["Order"]
    end

    subgraph COR["Saga COREOGRAFADA"]
        ORD2["Order"] -->|"OrderPlaced"| K1[("Kafka")]
        K1 --> PAY2["Payment"]
        PAY2 -->|"PaymentConfirmed"| K2[("Kafka")]
        K2 --> INV2["Inventory"]
    end
```

### Camino feliz vs compensación

```mermaid
sequenceDiagram
    autonumber
    participant S as OrderSaga
    participant P as Payment
    participant I as Inventory
    participant O as Order

    S->>P: charge()
    P-->>S: tx-abc123 ✅
    S->>I: reserve(stock)
    I-->>S: OK ✅
    S->>O: confirm()
    O-->>S: CONFIRMED ✅

    Note over S,O: ——— Si falla el paso 3 ———

    S->>P: charge()
    P-->>S: tx-xyz789 ✅
    S->>I: reserve(stock)
    I-->>S: SIN STOCK ❌
    S->>P: refund(tx-xyz789) 🔄 compensación
    S->>O: cancel("Sin stock")
    O-->>S: CANCELLED
```

**Clave:** las compensaciones se ejecutan en **orden inverso**. Consistencia **eventual**, no inmediata.

Ver código: `patterns/saga/` — `OrderSaga` orquesta pago → stock → confirmación, y compensa si
el stock no alcanza (reembolsa el pago). Corre el `App` y verás ambos caminos (éxito y compensación).

---

## CQRS

**CQRS** = *Command Query Responsibility Segregation*. Separa el modelo que **escribe**
(commands) del modelo que **lee** (queries).

**Problema:** el modelo rico de DDD (agregados con invariantes) es excelente para escribir, pero
malísimo para leer (una pantalla de "mis pedidos" necesitaría cargar agregados enteros y unir
datos de varios contextos). Además, las lecturas suelen ser 10-100x más frecuentes que las escrituras.

**Solución:** dos modelos.
- **Write model:** agregados DDD, normalizado, enfocado en invariantes.
- **Read model:** vistas desnormalizadas, optimizadas por pantalla (incluso en otra BD: Redis,
  Elasticsearch, una vista materializada).

Se sincronizan con **eventos de dominio** (aquí conecta con Outbox y Event Sourcing).

```mermaid
flowchart LR
    subgraph WRITE["Write Side (Commands)"]
        CMD["PlaceOrderCommand"]
        AGG["Agregado Order<br/>normalizado, invariantes"]
        DBW[("PostgreSQL<br/>write DB")]
        CMD --> AGG --> DBW
    end

    subgraph SYNC["Sincronización"]
        EVT["OrderPlaced<br/>OrderConfirmed"]
        PROJ["OrderProjector"]
        AGG --> EVT --> PROJ
    end

    subgraph READ["Read Side (Queries)"]
        RM["OrderSummaryReadModel<br/>desnormalizado, rápido"]
        DBR[("Read Store<br/>Redis / vista materializada")]
        QRY["GET /mis-pedidos"]
        PROJ --> RM --> DBR
        QRY --> DBR
    end
```

**No siempre lo necesitas.** CQRS agrega complejidad (consistencia eventual entre lectura y
escritura). Úsalo cuando lecturas y escrituras tengan requisitos muy distintos.

Ver código: `patterns/cqrs/` — `PlaceOrderCommandHandler` escribe; cuando el pedido se coloca,
se proyecta a un `OrderSummaryReadModel` que `OrderQueries` consulta rápido.

---

## Transactional Outbox

**Problema (dual write):** al colocar un pedido necesito **(1) guardar en la BD** y **(2)
publicar un evento en Kafka**. Si guardo y luego Kafka falla, tengo un pedido sin evento
(inventario nunca se entera). Si publico y la BD hace rollback, evento fantasma. No puedes
hacer commit atómico sobre dos sistemas distintos.

**Solución:** en la **misma transacción** de BD, escribe el cambio de negocio **y** una fila en
una tabla `outbox`. Un proceso aparte (poller o CDC con Debezium) lee la tabla `outbox` y publica
a Kafka, marcando cada fila como enviada. Así el evento se publica **at-least-once** garantizado.

```mermaid
sequenceDiagram
    participant App as OrderApplicationService
    participant DB as PostgreSQL
    participant Relay as OutboxRelay
    participant K as Kafka
    participant Inv as Inventory Service

    Note over App,DB: Una sola transacción ACID
    App->>DB: INSERT orders (...)
    App->>DB: INSERT outbox (OrderPlaced)
    App->>DB: COMMIT ✅

    Note over Relay,K: Proceso separado (poller / Debezium)
    Relay->>DB: SELECT * FROM outbox WHERE published=false
    Relay->>K: publish(OrderPlaced)
    Relay->>DB: UPDATE outbox SET published=true
    K->>Inv: consume (at-least-once)
```

### El problema del dual-write (sin Outbox)

```mermaid
flowchart TB
    subgraph SIN["❌ Sin Outbox — dual write peligroso"]
        A1["Guardar pedido en BD ✅"]
        A2["Publicar a Kafka ❌ falla"]
        A1 --> PROB["Pedido existe pero<br/>Inventory nunca se entera"]
    end

    subgraph CON["✅ Con Outbox — atómico"]
        B1["Guardar pedido + outbox<br/>en 1 transacción"]
        B2["Relay publica después"]
        B1 --> B2 --> OK["Consistencia garantizada"]
    end
```

**Consecuencia:** entrega *at-least-once* → los consumidores deben ser **idempotentes**.

Ver código: `patterns/outbox/` — `OrderApplicationService` guarda pedido + outbox atómicamente
(simulado en memoria) y un `OutboxRelay` publica los pendientes.

---

## API Gateway

**Problema:** si cada cliente llama directo a 20 microservicios: duplica auth, CORS, rate
limiting, versionado y descubre las direcciones internas.

**Solución:** un único **punto de entrada** que enruta a los servicios internos y centraliza
preocupaciones transversales:
- Enrutamiento y balanceo
- Autenticación/autorización (validar JWT)
- Rate limiting y throttling
- TLS termination, CORS, WAF básico
- Agregación simple de respuestas

En el curso: **Spring Cloud Gateway** y **Kong** (Módulo 7); en cloud, **AWS API Gateway** y
**Azure API Management**.

```mermaid
flowchart TB
    MOB["App Móvil"]
    WEB["Web SPA"]
    ADM["Admin Panel"]

    GW["API Gateway<br/>• JWT validation<br/>• Rate limiting<br/>• Routing<br/>• CORS / TLS"]

    CAT["Catalog Service"]
    ORD["Order Service"]
    PAY["Payment Service"]

    MOB --> GW
    WEB --> GW
    ADM --> GW
    GW --> CAT
    GW --> ORD
    GW --> PAY
```

> Cuidado: el gateway **no** debe contener lógica de negocio (si no, se vuelve un mini-monolito).

---

## BFF — Backend For Frontend

**Problema:** un API Gateway genérico no cubre que la **app móvil** necesita payloads pequeños y
pocas llamadas, mientras la **web** o el **panel admin** necesitan datos más ricos. Un solo API
para todos termina siendo un mal compromiso (viola ISP — doc 1).

**Solución:** un backend **por experiencia de cliente**. Cada BFF agrega/adapta datos de varios
servicios para su frontend específico.

```mermaid
flowchart TB
    subgraph CLIENTES["Frontends"]
        M["📱 App Móvil<br/>payload mínimo"]
        W["🌐 Web SPA<br/>datos ricos"]
        A["⚙️ Admin<br/>CRUD completo"]
    end

    subgraph BFFS["BFF — uno por experiencia"]
        BFFM["BFF Móvil<br/>1 call = checkout completo"]
        BFFW["BFF Web<br/>catálogo + recomendaciones"]
        BFFA["BFF Admin<br/>reportes + gestión"]
    end

    subgraph SVC["Microservicios internos"]
        CAT2["Catalog"]
        ORD2["Order"]
        PAY2["Payment"]
    end

    M --> BFFM
    W --> BFFW
    A --> BFFA
    BFFM --> CAT2
    BFFM --> ORD2
    BFFW --> CAT2
    BFFA --> ORD2
    BFFA --> PAY2
```

**BFF vs Gateway:** el Gateway es infraestructura genérica (enruta todo); el BFF es *tuyo*,
tiene lógica de agregación específica para *una* UI. A menudo conviven: cliente → Gateway → BFF.

---

## Strangler Fig

**Problema:** tienes un **monolito legacy** en producción. Reescribirlo de cero ("big bang") es
la receta clásica del desastre.

**Solución (Martin Fowler):** como la higuera estranguladora que crece alrededor de un árbol
hasta reemplazarlo, pones un **proxy/gateway delante del monolito** y vas migrando funcionalidad
pieza por pieza a nuevos microservicios. El proxy decide, ruta por ruta, si va al legacy o al
nuevo servicio. Cuando todo migró, apagas el monolito.

```mermaid
flowchart TB
    subgraph T1["Fase 1 — Todo al monolito"]
        C1["Clientes"] --> P1["Proxy"] --> MONO["Monolito Legacy"]
    end

    subgraph T2["Fase 2 — Migrando catálogo"]
        C2["Clientes"] --> P2["Proxy / Gateway"]
        P2 -->|" /catalog →"| NEW1["Catalog µsvc ✅"]
        P2 -->|" /* →"| MONO2["Monolito<br/>(orders, payments...)"]
    end

    subgraph T3["Fase 3 — Monolito apagado"]
        C3["Clientes"] --> GW3["API Gateway"]
        GW3 --> NEW2["Catalog ✅"]
        GW3 --> NEW3["Order ✅"]
        GW3 --> NEW4["Payment ✅"]
        MONO3["Monolito ❌ apagado"]
    end
```

Compañeros de viaje: un **Anticorruption Layer (ACL)** para que el modelo del legacy no
contamine a los servicios nuevos (DDD, doc 2).

---

## Cómo se combinan en el caso práctico

En la plataforma e-commerce (doc 5) verás casi todos juntos:

- **API Gateway** al frente → enruta a **BFFs** (web/móvil/admin).
- Colocar un pedido dispara una **Saga** (pago → stock → envío) con **compensaciones**.
- Cada servicio publica eventos vía **Outbox** a Kafka.
- La pantalla "mis pedidos" usa un **read model** (CQRS) alimentado por esos eventos.
- Si integramos un ERP legacy, lo hacemos con **Strangler Fig + ACL**.

```mermaid
flowchart TB
    GW["API Gateway"] --> BFF["BFFs"]
    BFF --> ORD["Order Service"]
    ORD -->|"Outbox"| KAFKA[("Kafka")]
    ORD --> SAGA["Saga<br/>Pay → Stock → Ship"]
    KAFKA --> CQRS["CQRS Projector<br/>→ Read Model"]
    KAFKA --> NOTIF["Notification"]
    LEGACY["ERP Legacy"] -.->|"Strangler + ACL"| GW
```

---

## Ejercicios

1. Dibuja la Saga de "devolución de pedido" (return) con sus compensaciones.
2. Diseña el read model para la pantalla "detalle de pedido". ¿Qué campos desnormalizas?
3. Explica por qué Outbox obliga a que los consumidores sean idempotentes y cómo lo lograrías.
