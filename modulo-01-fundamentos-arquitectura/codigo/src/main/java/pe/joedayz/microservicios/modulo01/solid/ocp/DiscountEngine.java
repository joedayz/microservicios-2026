package pe.joedayz.microservicios.modulo01.solid.ocp;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;

import java.util.List;

/**
 * OCP: motor de descuentos. Recorre las reglas configuradas y suma las que aplican.
 *
 * <p>Fijate que este codigo NO cambia cuando agregas una regla nueva: solo agregas
 * otra {@link DiscountRule} a la lista. Sin {@code if/else} gigantes ni {@code switch}
 * por tipo de descuento.
 */
public class DiscountEngine {

    private final List<DiscountRule> rules;

    public DiscountEngine(List<DiscountRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Money totalDiscount(Order order) {
        Money total = Money.zero(order.total().currency());
        for (DiscountRule rule : rules) {
            if (rule.applies(order)) {
                total = total.plus(rule.discountFor(order));
            }
        }
        return total;
    }
}
