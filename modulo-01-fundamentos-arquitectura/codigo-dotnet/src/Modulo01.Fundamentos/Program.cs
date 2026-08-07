using JoeDayz.Microservicios.Modulo01.Application;
using JoeDayz.Microservicios.Modulo01.Ddd.Orders;
using JoeDayz.Microservicios.Modulo01.Ddd.Shared;
using JoeDayz.Microservicios.Modulo01.Infrastructure;
using JoeDayz.Microservicios.Modulo01.Messaging;
using JoeDayz.Microservicios.Modulo01.Patterns.Cqrs;
using JoeDayz.Microservicios.Modulo01.Patterns.Outbox;
using JoeDayz.Microservicios.Modulo01.Patterns.Saga;
using JoeDayz.Microservicios.Modulo01.Solid.Dip;
using JoeDayz.Microservicios.Modulo01.Solid.Isp;
using JoeDayz.Microservicios.Modulo01.Solid.Liskov;
using JoeDayz.Microservicios.Modulo01.Solid.Ocp;
using JoeDayz.Microservicios.Modulo01.Solid.Ocp.Rules;
using JoeDayz.Microservicios.Modulo01.Solid.Srp;
using JoeDayz.Microservicios.Modulo01.Tenant;

namespace JoeDayz.Microservicios.Modulo01;

/// <summary>
/// Runner del Modulo 1 en .NET 10. Ejecuta las mismas demos que la version Java:
/// <code>
///   cd codigo-dotnet
///   dotnet run --project src/Modulo01.Fundamentos
/// </code>
/// </summary>
internal sealed class Demos
{
    // Composition root: aqui se enchufan puertos con adaptadores (DIP).
    // En una app real esto seria builder.Services.AddSingleton&lt;...&gt;() en Program.cs.
    private readonly IOrderRepository _orderRepository = new InMemoryOrderRepository();
    private readonly OutboxStore _outbox = new();
    private readonly EventBus _eventBus = new();
    private readonly OrderReadStore _readStore = new();
    private readonly OrderQueries _orderQueries;
    private readonly OrderApplicationService _orderAppService;
    private readonly OutboxRelay _outboxRelay;
    private readonly InMemoryInventoryAdapter _inventory = new();
    private readonly OrderSaga _orderSaga;
    private readonly OrderNotificationService _notifications = new();

    private Demos()
    {
        _orderQueries = new OrderQueries(_readStore);
        _orderAppService = new OrderApplicationService(_orderRepository, _outbox);
        _outboxRelay = new OutboxRelay(_outbox, _eventBus);
        _orderSaga = new OrderSaga(new InMemoryPaymentAdapter(), _inventory, _orderAppService);

        // El proyector CQRS se suscribe a los eventos que salen por la outbox.
        var projector = new OrderProjector(_readStore);
        _eventBus.Subscribe(projector.On);
    }

    private static void Main()
    {
        var demos = new Demos();
        Banner("MODULO 1 - FUNDAMENTOS Y ARQUITECTURA · .NET 10 (JoeDayz.pe)");
        demos.DemoSolid();
        demos.DemoHappyPath();
        demos.DemoCompensation();
        Console.WriteLine("\nFin de las demos. Revisa la teoria en ../docs/.");
    }

    /// <summary>SOLID: SRP + OCP + LSP + ISP + DIP.</summary>
    private void DemoSolid()
    {
        Section("1) SOLID en accion (SRP + OCP + LSP + ISP + DIP)");
        TenantContext.Set(TenantId.Of("tienda-deportes"));

        var order = Order.Create(TenantContext.Require(), CustomerId.Of("cliente-001"), "PEN");
        order.AddLine(Sku.Of("ZAP-RUN-42"), Quantity.Of(1), Money.Of(300.00m, "PEN"));
        order.AddLine(Sku.Of("MEDIAS-01"), Quantity.Of(3), Money.Of(20.00m, "PEN"));

        // OCP: agregar reglas nuevas NO modifica el DiscountEngine.
        var engine = new DiscountEngine([
            new PercentageRule(10m),
            new FreeShippingOverAmountRule(Money.Of(200.00m, "PEN"), Money.Of(15.00m, "PEN"))
        ]);

        // SRP: el servicio de precios solo calcula precios.
        var pricing = new OrderPricingService(engine);
        Console.WriteLine($"   Total bruto : {order.Total()}");
        Console.WriteLine($"   Descuentos  : {engine.TotalDiscount(order)}");
        Console.WriteLine($"   Precio final: {pricing.FinalPrice(order)}");

        // LSP: dos gateways distintos, el mismo Checkout, sin ifs por tipo.
        new Checkout(new StripeGateway()).Process(100m);
        new Checkout(new CulqiGateway()).Process(100m);

        // ISP: el storefront solo usa ICatalogReadApi; el admin solo ICatalogAdminApi.
        var catalog = new InMemoryCatalogService();
        var storefront = new StorefrontBff(catalog);
        var admin = new AdminBff(catalog);
        Console.WriteLine($"   [ISP] Storefront ve {storefront.HomePage().Count} productos publicados (sin borradores)");
        Console.WriteLine($"   [ISP] Admin ve {admin.Dashboard().Count} productos (incluye borradores)");

        // DIP: cambiar de motor de BD es cambiar el adaptador registrado.
        new OrderPersistenceService(new SqlServerOrderStore()).ProcessOrder("demo-dip");

        TenantContext.Clear();
    }

    /// <summary>Camino feliz: DDD -> Outbox -> broker -> CQRS -> Saga confirmada.</summary>
    private void DemoHappyPath()
    {
        Section("2) Checkout OK: DDD -> Outbox -> Kafka -> CQRS -> Saga (confirmado)");
        TenantContext.Set(TenantId.Of("tienda-deportes"));
        var tenant = TenantContext.Require();

        _inventory.SetStock(tenant, Sku.Of("ZAP-RUN-42"), 10);

        // DDD: el agregado valida sus invariantes al construirse y al mutar.
        var order = Order.Create(tenant, CustomerId.Of("cliente-001"), "PEN");
        order.AddLine(Sku.Of("ZAP-RUN-42"), Quantity.Of(2), Money.Of(300.00m, "PEN"));
        order.Place();

        // Outbox: pedido + eventos en la misma transaccion, luego relay al broker.
        _orderAppService.SaveWithEvents(order);
        _outboxRelay.RelayPending();
        PrintReadModel(tenant, order);

        // Saga: cobra, reserva, confirma.
        var result = _orderSaga.Execute(order);
        _outboxRelay.RelayPending();
        Console.WriteLine($"   Resultado Saga: {result.Detail}");
        PrintReadModel(tenant, order);

        if (order.Status is OrderStatus.Confirmed)
        {
            _notifications.NotifyConfirmed(order);
        }

        TenantContext.Clear();
    }

    /// <summary>Camino de fallo: la Saga compensa (reembolso) y cancela el pedido.</summary>
    private void DemoCompensation()
    {
        Section("3) Checkout con fallo de stock: Saga compensa (reembolso + cancelacion)");
        TenantContext.Set(TenantId.Of("libreria-lima"));
        var tenant = TenantContext.Require();

        // Stock insuficiente a proposito: pedimos 5 y solo hay 1.
        _inventory.SetStock(tenant, Sku.Of("LIB-DDD-01"), 1);

        var order = Order.Create(tenant, CustomerId.Of("cliente-777"), "PEN");
        order.AddLine(Sku.Of("LIB-DDD-01"), Quantity.Of(5), Money.Of(120.00m, "PEN"));
        order.Place();
        _orderAppService.SaveWithEvents(order);
        _outboxRelay.RelayPending();
        PrintReadModel(tenant, order);

        var result = _orderSaga.Execute(order);
        _outboxRelay.RelayPending();
        Console.WriteLine($"   Resultado Saga: {result.Detail}");
        PrintReadModel(tenant, order);

        TenantContext.Clear();
    }

    private void PrintReadModel(TenantId tenant, Order order)
    {
        var view = _orderQueries.FindSummary(tenant.Value, order.Id.ToString());
        if (view is null)
        {
            Console.WriteLine("   [CQRS read model] aun sin proyeccion");
            return;
        }

        Console.WriteLine(
            $"   [CQRS read model] pedido={view.OrderId[..8]} total={view.Total} estado={view.Status}");
    }

    private static void Banner(string text)
    {
        Console.WriteLine($"\n{new string('=', 60)}");
        Console.WriteLine($" {text}");
        Console.WriteLine(new string('=', 60));
    }

    private static void Section(string text)
    {
        Console.WriteLine($"\n{new string('-', 60)}");
        Console.WriteLine($" {text}");
        Console.WriteLine(new string('-', 60));
    }
}
