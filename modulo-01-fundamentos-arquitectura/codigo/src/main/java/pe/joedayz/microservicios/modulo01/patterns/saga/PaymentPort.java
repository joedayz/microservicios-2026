package pe.joedayz.microservicios.modulo01.patterns.saga;

import pe.joedayz.microservicios.modulo01.ddd.order.OrderId;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

/**
 * Puerto hacia el servicio de Pagos (Stripe/Culqi en el curso). Incluye la operacion de
 * COMPENSACION {@link #refund} que la Saga usa para deshacer un cobro si un paso posterior falla.
 */
public interface PaymentPort {

    /** @return id de la transaccion si el cobro fue exitoso. */
    String charge(TenantId tenantId, OrderId orderId, Money amount);

    /** Compensacion: reembolsa un cobro previo. */
    void refund(TenantId tenantId, String transactionId);
}
