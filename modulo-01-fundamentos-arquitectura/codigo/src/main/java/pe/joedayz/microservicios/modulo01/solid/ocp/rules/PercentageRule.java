package pe.joedayz.microservicios.modulo01.solid.ocp.rules;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.solid.ocp.DiscountRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regla: descuento porcentual sobre el total. Ejemplo de extension nueva agregada
 * SIN tocar el {@link pe.joedayz.microservicios.modulo01.solid.ocp.DiscountEngine}.
 */
public class PercentageRule implements DiscountRule {

    private final BigDecimal percentage;

    public PercentageRule(BigDecimal percentage) {
        this.percentage = percentage;
    }

    @Override
    public boolean applies(Order order) {
        return true;
    }

    @Override
    public Money discountFor(Order order) {
        BigDecimal amount = order.total().amount()
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN);
        return new Money(amount, order.total().currency());
    }

    @Override
    public String name() {
        return "Descuento del " + percentage + "%";
    }
}
