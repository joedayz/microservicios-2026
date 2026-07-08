package pe.joedayz.microservicios.modulo01.patterns.saga;

import pe.joedayz.microservicios.modulo01.app.OrderApplicationService;
import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.order.OrderLine;
import pe.joedayz.microservicios.modulo01.solid.dip.InventoryPort;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * SAGA ORQUESTADA del checkout. Coordina una secuencia de transacciones locales entre
 * servicios (Pago, Inventario) que NO comparten transaccion. Si un paso falla, ejecuta las
 * COMPENSACIONES en orden inverso (undo semantico). Resultado: consistencia eventual.
 *
 * <pre>
 *   1) Cobrar pago        (compensacion: reembolsar)
 *   2) Reservar stock     (compensacion: liberar stock)
 *   3) Confirmar pedido
 *   Si 2 falla -> se compensa 1 y se cancela el pedido.
 * </pre>
 *
 * <p>Usamos una pila de compensaciones: cada paso exitoso apila su "deshacer", y ante un
 * fallo se desapila ejecutando cada compensacion. Es el esqueleto de lo que en el Modulo 4
 * haras con Kafka y estados persistidos.
 */
public class OrderSaga {

    private final PaymentPort paymentPort;
    private final InventoryPort inventoryPort;
    private final OrderApplicationService orderApplicationService;

    public OrderSaga(PaymentPort paymentPort,
                     InventoryPort inventoryPort,
                     OrderApplicationService orderApplicationService) {
        this.paymentPort = paymentPort;
        this.inventoryPort = inventoryPort;
        this.orderApplicationService = orderApplicationService;
    }

    public SagaResult execute(Order order) {
        Deque<Runnable> compensations = new ArrayDeque<>();
        try {
            // Paso 1: cobrar el pago
            String txId = paymentPort.charge(order.tenantId(), order.id(), order.total());
            compensations.push(() -> paymentPort.refund(order.tenantId(), txId));

            // Paso 2: reservar stock de cada linea
            for (OrderLine line : order.lines()) {
                boolean reserved = inventoryPort.reserve(order.tenantId(), line.sku(), line.quantity());
                if (!reserved) {
                    throw new SagaStepFailedException(
                            "Sin stock para " + line.sku() + " x" + line.quantity());
                }
                System.out.printf("   [Inventory] reservadas %s uds de %s%n", line.quantity(), line.sku());
                compensations.push(() ->
                        inventoryPort.release(order.tenantId(), line.sku(), line.quantity()));
            }

            // Paso 3: confirmar el pedido (emite OrderConfirmed)
            order.confirm();
            orderApplicationService.saveWithEvents(order);
            return SagaResult.confirmed();

        } catch (SagaStepFailedException failure) {
            System.out.printf("   [Saga] FALLO: %s -> compensando...%n", failure.getMessage());
            while (!compensations.isEmpty()) {
                compensations.pop().run();
            }
            order.cancel(failure.getMessage());
            orderApplicationService.saveWithEvents(order);
            return SagaResult.cancelled(failure.getMessage());
        }
    }
}
