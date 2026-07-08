# 1. Principios SOLID para microservicios

Los principios **SOLID** los popularizó Robert C. Martin para el diseño orientado a objetos.
En microservicios siguen siendo válidos a nivel de clase, pero además **se elevan a nivel de
servicio**: cada principio tiene una lectura "en pequeño" (dentro del código) y otra "en grande"
(entre servicios).

| Sigla | Principio | Lectura en microservicios |
|-------|-----------|---------------------------|
| **S** | Single Responsibility | Un servicio = una capability de negocio (un bounded context) |
| **O** | Open/Closed | Extender vía eventos/plugins sin tocar servicios existentes |
| **L** | Liskov Substitution | Un contrato (API/evento) versionado debe seguir siendo sustituible |
| **I** | Interface Segregation | APIs finas y específicas por consumidor (BFF), no un "God API" |
| **D** | Dependency Inversion | Depende de puertos/contratos, no de implementaciones (Hexagonal) |

### Mapa mental: SOLID en dos niveles

```mermaid
flowchart TB
    subgraph NIVEL_CLASE["Nivel clase (dentro del microservicio)"]
        S1["S — Una clase, una responsabilidad"]
        O1["O — Extender con nuevas clases/reglas"]
        L1["L — Subtipos respetan el contrato"]
        I1["I — Interfaces pequeñas y específicas"]
        D1["D — Depender de interfaces, no de JDBC/HTTP"]
    end

    subgraph NIVEL_SERVICIO["Nivel servicio (entre microservicios)"]
        S2["S — Un servicio = un bounded context"]
        O2["O — Extender con nuevos consumidores de eventos"]
        L2["L — APIs/eventos versionados y compatibles"]
        I2["I — BFF con APIs finas por frontend"]
        D2["D — Puertos hexagonales + adaptadores"]
    end

    S1 -.->|"se eleva a"| S2
    O1 -.->|"se eleva a"| O2
    L1 -.->|"se eleva a"| L2
    I1 -.->|"se eleva a"| I2
    D1 -.->|"se eleva a"| D2
```

---

## S — Single Responsibility Principle (SRP)

> "Una clase debe tener una sola razón para cambiar."

En objetos: una clase no debe mezclar reglas de negocio, persistencia y notificaciones.

En microservicios: **un servicio debe pertenecer a un único bounded context**. Si al cambiar
la política de envíos tienes que redeployar el servicio de pagos, tus responsabilidades están
mal repartidas.

**Síntoma de violación:** el "monolito distribuido" — muchos servicios que siempre se
despliegan juntos porque comparten responsabilidades.

Ver código: `solid/srp/` — separamos `OrderPricingService` (calcula precios) de
`OrderNotificationService` (notifica), en vez de un `OrderManager` que hace todo.

### Antes vs después (SRP)

```mermaid
flowchart LR
    subgraph MAL["❌ OrderManager — 3 razones de cambio"]
        OM["OrderManager"]
        OM --> P["Calcular precios"]
        OM --> N["Enviar emails"]
        OM --> DB["Guardar en BD"]
    end

    subgraph BIEN["✅ Servicios con una sola responsabilidad"]
        OPS["OrderPricingService"]
        ONS["OrderNotificationService"]
        REPO["OrderRepository"]
        OPS --> P2["Solo precios"]
        ONS --> N2["Solo notificaciones"]
        REPO --> DB2["Solo persistencia"]
    end
```

> **Regla para alumnos:** si al cambiar la lógica de descuentos también tienes que tocar el código
> de emails, estás violando SRP.

---

## O — Open/Closed Principle (OCP)

> "Abierto a extensión, cerrado a modificación."

En microservicios el mejor mecanismo de extensión es la **arquitectura orientada a eventos**:
cuando se crea un pedido, el servicio de pedidos publica `OrderPlaced`. Mañana quieres sumar
un servicio de "programa de puntos": lo suscribes al evento **sin tocar** el servicio de pedidos.

Ver código: `solid/ocp/` — cálculo de descuentos con una lista de `DiscountRule`; añadir una
regla nueva no modifica el motor.

### Extensión sin modificar (OCP)

```mermaid
flowchart TB
    subgraph HOY["Hoy — sin tocar DiscountEngine"]
        DE["DiscountEngine"]
        R1["PercentageRule 10%"]
        R2["FreeShippingOverAmountRule"]
        DE --> R1
        DE --> R2
    end

    subgraph MANANA["Mañana — solo agregas una clase nueva"]
        R3["BlackFridayRule 🆕"]
        DE2["DiscountEngine<br/>(sin cambios)"] --> R1b["PercentageRule"]
        DE2 --> R2b["FreeShippingRule"]
        DE2 --> R3
    end
```

En microservicios, el equivalente es suscribir un **nuevo consumidor** a `OrderPlaced` sin
modificar el servicio de Pedidos:

```mermaid
flowchart LR
    ORDER["Order Service"] -->|"OrderPlaced"| KAFKA[("Kafka")]
    KAFKA --> INV["Inventory"]
    KAFKA --> NOTIF["Notification"]
    KAFKA --> LOYALTY["Loyalty 🆕<br/>(nuevo, sin tocar Order)"]
```

---

## L — Liskov Substitution Principle (LSP)

> "Los subtipos deben poder sustituir a su tipo base sin romper el programa."

En microservicios esto es **compatibilidad de contratos**. La versión `v2` de una API o de un
evento debe poder sustituir a `v1` para los consumidores existentes (backward compatibility):
no elimines campos, no cambies semántica, solo agrega opcionales.

**Regla práctica (Tolerant Reader / Postel's Law):** *sé estricto en lo que emites, tolerante
en lo que aceptas.*

Ver código: `solid/lsp/` — un `PaymentGateway` cuyas implementaciones (`StripeGateway`,
`CulqiGateway`) respetan el mismo contrato y post-condiciones.

### Versionado compatible de eventos (LSP)

```mermaid
flowchart TB
    subgraph V1["OrderPlaced v1 (consumidores actuales)"]
        E1["tenantId<br/>orderId<br/>total"]
    end

    subgraph V2["OrderPlaced v2 (compatible hacia atrás)"]
        E2["tenantId<br/>orderId<br/>total<br/>currency 🆕 opcional<br/>channel 🆕 opcional"]
    end

    V1 -->|"agregar campos opcionales ✅"| V2
    V1 -.->|"eliminar 'total' ❌ rompe LSP"| X["Consumidores fallan"]
```

---

## I — Interface Segregation Principle (ISP)

> "Ningún cliente debería depender de métodos que no usa."

En microservicios: evita el **God API** que sirve a todos. La app móvil, la web y el panel
admin tienen necesidades distintas. En vez de una interfaz gorda, ofrece interfaces finas
(esto justifica el patrón **BFF – Backend For Frontend**).

Ver código: `solid/isp/` — separamos `CatalogReadApi` (lo que necesita el storefront) de
`CatalogAdminApi` (lo que necesita el back-office).

### God API vs interfaces finas (ISP)

```mermaid
flowchart TB
    subgraph GOD["❌ God API — el móvil recibe lo que no usa"]
        GA["/api/catalog<br/>GET · POST · PUT · DELETE<br/>+ admin + SEO + stock"]
        MOB1["App Móvil"] --> GA
        WEB1["Web"] --> GA
        ADM1["Admin"] --> GA
    end

    subgraph FINO["✅ Interfaces finas — cada cliente usa lo suyo"]
        CRA["CatalogReadApi<br/>solo lectura, payload liviano"]
        CAA["CatalogAdminApi<br/>CRUD completo"]
        MOB2["App Móvil"] --> CRA
        WEB2["Web"] --> CRA
        ADM2["Admin"] --> CAA
    end
```

---

## D — Dependency Inversion Principle (DIP)

> "Depende de abstracciones, no de concreciones."

Es la base de la **Arquitectura Hexagonal (Ports & Adapters)**: el núcleo de dominio define
**puertos** (interfaces) y los detalles (base de datos, Kafka, REST) son **adaptadores**
enchufables. El dominio no importa Spring ni JDBC.

### Arquitectura Hexagonal (Ports & Adapters)

```mermaid
flowchart TB
    subgraph ADAPTADORES_ENTRADA["Adaptadores de entrada"]
        REST["REST Controller"]
        KAFKA_IN["Kafka Consumer"]
    end

    subgraph DOMINIO["Núcleo de dominio (sin Spring, sin JDBC)"]
        SVC["OrderSaga / OrderService"]
        PORT1["InventoryPort"]
        PORT2["PaymentPort"]
        PORT3["OrderRepository"]
        SVC --> PORT1
        SVC --> PORT2
        SVC --> PORT3
    end

    subgraph ADAPTADORES_SALIDA["Adaptadores de salida (intercambiables)"]
        MEM["InMemoryInventoryAdapter<br/>(tests)"]
        GRPC["GrpcInventoryAdapter<br/>(producción)"]
        JPA["JpaOrderRepository"]
        PAY["StripePaymentAdapter"]
    end

    REST --> SVC
    KAFKA_IN --> SVC
    PORT1 --> MEM
    PORT1 --> GRPC
    PORT2 --> PAY
    PORT3 --> JPA
```

Ver código: `solid/dip/` — `OrderSaga` depende del puerto `InventoryPort`, no de un cliente
HTTP concreto. En test usamos un adaptador en memoria; en prod, uno que llama al servicio real.

```mermaid
flowchart LR
    TEST["Entorno test"] --> MEM["InMemoryInventoryAdapter"]
    PROD["Entorno prod"] --> GRPC["GrpcInventoryAdapter"]
    MEM --> PORT["InventoryPort (misma interfaz)"]
    GRPC --> PORT
    PORT --> SAGA["OrderSaga"]
```

---

## Errores comunes al aplicar SOLID en microservicios

- **Servicios anémicos ultra-pequeños** ("nanoservicios"): tanta SRP que un caso de uso simple
  requiere 6 saltos de red. SRP es sobre *responsabilidad de negocio*, no sobre líneas de código.
- **Compartir base de datos** entre servicios: rompe SRP y DIP a la vez (acoplamiento por datos).
- **Contratos frágiles**: romper LSP al eliminar campos causa incidentes en cascada.

---

## Ejercicios

1. Toma el `OrderManager` de `solid/srp` y detecta sus 3 razones de cambio. Sepáralas.
2. Agrega una nueva `DiscountRule` (ej. "envío gratis > S/200") sin tocar el motor (OCP).
3. Diseña la versión `v2` del evento `OrderPlaced` agregando un campo sin romper `v1` (LSP).
