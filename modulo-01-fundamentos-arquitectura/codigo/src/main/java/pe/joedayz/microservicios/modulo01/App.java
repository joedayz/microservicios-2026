package pe.joedayz.microservicios.modulo01;

import pe.joedayz.microservicios.modulo01.app.OrderApplicationService;
import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.order.CustomerId;
import pe.joedayz.microservicios.modulo01.ddd.order.OrderRepository;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.ddd.shared.Quantity;
import pe.joedayz.microservicios.modulo01.ddd.shared.Sku;
import pe.joedayz.microservicios.modulo01.infra.InMemoryOrderRepository;
import pe.joedayz.microservicios.modulo01.messaging.EventBus;
import pe.joedayz.microservicios.modulo01.patterns.cqrs.OrderProjector;
import pe.joedayz.microservicios.modulo01.patterns.cqrs.OrderQueries;
import pe.joedayz.microservicios.modulo01.patterns.cqrs.OrderReadStore;
import pe.joedayz.microservicios.modulo01.patterns.cqrs.OrderSummaryReadModel;
import pe.joedayz.microservicios.modulo01.patterns.outbox.OutboxRelay;
import pe.joedayz.microservicios.modulo01.patterns.outbox.OutboxStore;
import pe.joedayz.microservicios.modulo01.patterns.saga.InMemoryPaymentAdapter;
import pe.joedayz.microservicios.modulo01.patterns.saga.OrderSaga;
import pe.joedayz.microservicios.modulo01.patterns.saga.PaymentPort;
import pe.joedayz.microservicios.modulo01.patterns.saga.SagaResult;
import pe.joedayz.microservicios.modulo01.solid.dip.InMemoryInventoryAdapter;
import pe.joedayz.microservicios.modulo01.solid.isp.AdminBff;
import pe.joedayz.microservicios.modulo01.solid.isp.InMemoryCatalogService;
import pe.joedayz.microservicios.modulo01.solid.isp.StorefrontBff;
import pe.joedayz.microservicios.modulo01.solid.ocp.DiscountEngine;
import pe.joedayz.microservicios.modulo01.solid.ocp.rules.FreeShippingOverAmountRule;
import pe.joedayz.microservicios.modulo01.solid.ocp.rules.PercentageRule;
import pe.joedayz.microservicios.modulo01.solid.srp.OrderNotificationService;
import pe.joedayz.microservicios.modulo01.solid.srp.OrderPricingService;
import pe.joedayz.microservicios.modulo01.tenant.TenantContext;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.math.BigDecimal;
import java.util.List;

/**
 * Runner del Modulo 1. Ejecuta todas las demos en orden.
 *
 * <pre>
 *   cd codigo
 *   mvn -q compile exec:java
 * </pre>
 */
public class App {

    // Cableado (composition root): aqui se enchufan puertos con adaptadores (DIP).
    private final OrderRepository orderRepository = new InMemoryOrderRepository();
    private final OutboxStore outbox = new OutboxStore();
    private final EventBus eventBus = new EventBus();
    private final OrderReadStore readStore = new OrderReadStore();
    private final OrderQueries orderQueries = new OrderQueries(readStore);
    private final OrderApplicationService orderAppService =
            new OrderApplicationService(orderRepository, outbox);
    private final OutboxRelay outboxRelay = new OutboxRelay(outbox, eventBus);
    private final InMemoryInventoryAdapter inventory = new InMemoryInventoryAdapter();
    private final PaymentPort payment = new InMemoryPaymentAdapter();
    private final OrderSaga orderSaga = new OrderSaga(payment, inventory, orderAppService);
    private final OrderNotificationService notifications = new OrderNotificationService();

    public App() {
        // El proyector de CQRS se suscribe a los eventos que salen por la outbox.
        OrderProjector projector = new OrderProjector(readStore);
        eventBus.subscribe(projector::on);
    }

    public static void main(String[] args) {
        App app = new App();
        banner("MODULO 1 - FUNDAMENTOS Y ARQUITECTURA (JoeDayz.pe)");
        app.demoSolid();
        app.demoHappyPath();
        app.demoCompensation();
        System.out.println("\nFin de las demos. Revisa la teoria en ../docs/.");
    }

    /** SOLID: OCP + SRP + ISP + DIP (ver paquetes solid/*). */
    private void demoSolid() {
        section("1) SOLID en accion (OCP + SRP + ISP)");
        TenantContext.set(TenantId.of("tienda-deportes"));

        Order order = Order.create(TenantContext.require(), CustomerId.of("cliente-001"), "PEN");
        order.addLine(Sku.of("ZAP-RUN-42"), Quantity.of(1), Money.of("300.00", "PEN"));
        order.addLine(Sku.of("MEDIAS-01"), Quantity.of(3), Money.of("20.00", "PEN"));

        // OCP: agregar reglas nuevas NO modifica el DiscountEngine.
        DiscountEngine engine = new DiscountEngine(List.of(
                new PercentageRule(new BigDecimal("10")),
                new FreeShippingOverAmountRule(Money.of("200.00", "PEN"), Money.of("15.00", "PEN"))
        ));
        // SRP: el servicio de precios solo calcula precios.
        OrderPricingService pricing = new OrderPricingService(engine);

        System.out.println("   Total bruto : " + order.total());
        System.out.println("   Descuentos  : " + engine.totalDiscount(order));
        System.out.println("   Precio final: " + pricing.finalPrice(order));

        // ISP: BFF storefront solo usa CatalogReadApi; admin solo CatalogAdminApi.
        InMemoryCatalogService catalog = new InMemoryCatalogService();
        StorefrontBff storefront = new StorefrontBff(catalog);
        AdminBff admin = new AdminBff(catalog);
        System.out.println("   [ISP] Storefront ve " + storefront.homePage().size()
                + " productos publicados (sin borradores)");
        System.out.println("   [ISP] Admin ve " + admin.dashboard().size()
                + " productos (incluye borradores)");

        TenantContext.clear();
    }

    /** Camino feliz: DDD + Outbox + Kafka + CQRS + Saga (todo confirmado). */
    private void demoHappyPath() {
        section("2) Checkout OK: DDD -> Outbox -> Kafka -> CQRS -> Saga (confirmado)");
        TenantContext.set(TenantId.of("tienda-deportes"));
        TenantId tenant = TenantContext.require();

        // Hay stock suficiente.
        inventory.setStock(tenant, Sku.of("ZAP-RUN-42"), 10);

        // DDD: construir el agregado a traves de la raiz (valida invariantes).
        Order order = Order.create(tenant, CustomerId.of("cliente-001"), "PEN");
        order.addLine(Sku.of("ZAP-RUN-42"), Quantity.of(2), Money.of("300.00", "PEN"));
        order.place(); // emite OrderPlaced

        // Outbox: guardar pedido + eventos atomicamente, luego relay a Kafka.
        orderAppService.saveWithEvents(order);
        outboxRelay.relayPending(); // publica OrderPlaced -> proyector CQRS actualiza read model

        printReadModel(tenant, order);

        // Saga: cobra, reserva, confirma; persiste OrderConfirmed via outbox.
        SagaResult result = orderSaga.execute(order);
        outboxRelay.relayPending(); // publica OrderConfirmed -> read model CONFIRMED
        System.out.println("   Resultado Saga: " + result.detail());

        printReadModel(tenant, order);
        if (order.status().name().equals("CONFIRMED")) {
            notifications.notifyConfirmed(order); // SRP: notificacion desacoplada
        }
        TenantContext.clear();
    }

    /** Camino de fallo: la Saga compensa (reembolsa pago) y cancela el pedido. */
    private void demoCompensation() {
        section("3) Checkout con fallo de stock: Saga compensa (reembolso + cancelacion)");
        TenantContext.set(TenantId.of("libreria-lima"));
        TenantId tenant = TenantContext.require();

        // Stock insuficiente a proposito: pedimos 5 y solo hay 1.
        inventory.setStock(tenant, Sku.of("LIB-DDD-01"), 1);

        Order order = Order.create(tenant, CustomerId.of("cliente-777"), "PEN");
        order.addLine(Sku.of("LIB-DDD-01"), Quantity.of(5), Money.of("120.00", "PEN"));
        order.place();
        orderAppService.saveWithEvents(order);
        outboxRelay.relayPending();
        printReadModel(tenant, order);

        SagaResult result = orderSaga.execute(order); // cobra, falla al reservar, compensa
        outboxRelay.relayPending();
        System.out.println("   Resultado Saga: " + result.detail());
        printReadModel(tenant, order);
        TenantContext.clear();
    }

    private void printReadModel(TenantId tenant, Order order) {
        OrderSummaryReadModel view = orderQueries.findSummary(tenant.value(), order.id().toString());
        System.out.printf("   [CQRS read model] pedido=%s total=%s estado=%s%n",
                shortId(view.orderId()), view.total(), view.status());
    }

    private static String shortId(String id) {
        return id.substring(0, 8);
    }

    private static void banner(String text) {
        System.out.println("\n============================================================");
        System.out.println(" " + text);
        System.out.println("============================================================");
    }

    private static void section(String text) {
        System.out.println("\n------------------------------------------------------------");
        System.out.println(" " + text);
        System.out.println("------------------------------------------------------------");
    }
}
