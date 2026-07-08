package pe.joedayz.microservicios.modulo01.patterns.saga;

import pe.joedayz.microservicios.modulo01.ddd.order.OrderId;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.util.UUID;

/** Adaptador simulado del {@link PaymentPort}. Registra en consola los cobros y reembolsos. */
public class InMemoryPaymentAdapter implements PaymentPort {

    @Override
    public String charge(TenantId tenantId, OrderId orderId, Money amount) {
        String txId = "tx-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.printf("   [Payment] cobrado %s por pedido %s -> %s%n", amount, orderId, txId);
        return txId;
    }

    @Override
    public void refund(TenantId tenantId, String transactionId) {
        System.out.printf("   [Payment] COMPENSACION: reembolsado %s%n", transactionId);
    }
}
