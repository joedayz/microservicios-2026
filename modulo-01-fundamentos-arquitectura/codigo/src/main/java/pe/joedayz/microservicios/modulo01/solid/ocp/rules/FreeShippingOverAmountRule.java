package pe.joedayz.microservicios.modulo01.solid.ocp.rules;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.solid.ocp.DiscountRule;

/** Regla: descuento fijo (equivalente a envio gratis) si el total supera un umbral. */
public class FreeShippingOverAmountRule implements DiscountRule {

    private final Money threshold;
    private final Money shippingSaved;

    public FreeShippingOverAmountRule(Money threshold, Money shippingSaved) {
        this.threshold = threshold;
        this.shippingSaved = shippingSaved;
    }

    @Override
    public boolean applies(Order order) {
        return order.total().isGreaterThan(threshold);
    }

    @Override
    public Money discountFor(Order order) {
        return shippingSaved;
    }

    @Override
    public String name() {
        return "Envio gratis por compra mayor a " + threshold;
    }
}
