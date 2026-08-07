# Módulo 1 – Demos en .NET 10

> Curso: **Arquitectura de Microservicios Pro** · JoeDayz.pe
> Port de las demos Java del módulo a **.NET 10 / C# 14**.

Este directorio es **independiente** de [`../codigo/`](../codigo/) (Java). Mismos conceptos,
mismo dominio (**e-commerce multi-tenant**), misma salida por consola — pero escrito de forma
idiomática en .NET, para equipos que trabajan con ambos stacks.

## Requisitos

- **.NET SDK 10.0.100+** (`dotnet --list-sdks`)

El [`global.json`](global.json) fija el SDK 10 con `rollForward: latestFeature`, así que
no interfiere con otros SDK instalados en la máquina.

## Cómo ejecutar

```bash
cd modulo-01-fundamentos-arquitectura/codigo-dotnet
dotnet run --project src/Modulo01.Fundamentos
```

Verás las tres demos: SOLID, checkout confirmado y checkout compensado por la Saga.

## Estructura

```
src/Modulo01.Fundamentos/
├── Program.cs                     # runner: ejecuta las 3 demos
├── Tenant/                        # multi-tenancy (TenantId + TenantContext con AsyncLocal)
├── Ddd/
│   ├── Shared/                    # Money, Quantity, Sku, IDomainEvent
│   └── Orders/                    # agregado Order, OrderLine, IOrderRepository, Events/
├── Application/                   # OrderApplicationService (caso de uso + outbox)
├── Infrastructure/                # InMemoryOrderRepository
├── Messaging/                     # EventBus (broker simulado)
├── Patterns/
│   ├── Outbox/                    # Transactional Outbox + relay
│   ├── Cqrs/                      # read model, proyector y queries
│   └── Saga/                      # Saga orquestada con compensaciones
└── Solid/
    ├── Srp/                       # OrderManager (malo) vs servicios separados
    ├── Ocp/                       # DiscountEngine + IDiscountRule (+ Rules/)
    ├── Liskov/                    # IPaymentGateway (Stripe, Culqi, BrokenGateway)
    ├── Isp/                       # ICatalogReadApi vs ICatalogAdminApi + BFFs
    └── Dip/                       # IInventoryPort + adaptadores
```

## Equivalencias Java ↔ .NET

Tabla útil si vienes del código Java del módulo:

| Concepto | Java 25 | .NET 10 / C# 14 |
|---|---|---|
| Value object inmutable | `record` | `record` / `readonly record struct` |
| Importe monetario | `BigDecimal` | `decimal` |
| Contexto de request | `ThreadLocal` / `ScopedValue` | `AsyncLocal<T>` (fluye por `await`) |
| Instante UTC | `Instant` | `DateTimeOffset` |
| `Optional<T>` | `Optional.empty()` | tipos anulables (`Order?`) + `is { } x` |
| Pattern matching sobre eventos | `switch (event) { case OrderPlaced e -> ... }` | `switch (e) { case OrderPlaced x: ... }` |
| Inyección de dependencias | constructor manual / Spring | primary constructors + `builder.Services` |
| Lista inmutable | `List.copyOf(...)` | collection expressions `[.. items]` |
| Validación de argumentos | `Objects.requireNonNull` | `ArgumentNullException.ThrowIfNull` |
| Regex validado | `String.matches` | `[GeneratedRegex]` (source generator) |
| Broker | Kafka + Spring Cloud Stream | Kafka / Azure Service Bus / Amazon SQS |

## Qué demuestra cada demo

1. **SOLID en acción** — SRP (precio/notificación separados), OCP (agregar reglas de
   descuento sin tocar el motor), LSP (dos gateways intercambiables), ISP (storefront ve
   2 productos, admin ve 3 incluyendo borradores) y DIP (cambiar de motor de BD = cambiar
   un registro en el contenedor).
2. **Checkout OK** — el agregado emite `OrderPlaced`, se guarda con sus eventos en la
   **outbox** (misma transacción), el relay publica al broker, el **proyector CQRS**
   actualiza el read model y la **Saga** cobra → reserva → confirma.
3. **Checkout con fallo** — no hay stock: la Saga ejecuta las **compensaciones** en orden
   inverso (reembolso) y cancela el pedido; el read model termina en `CANCELLED`.

---

*Teoría del módulo:* [`../docs/`](../docs/) · *Versión Java:* [`../codigo/`](../codigo/)
