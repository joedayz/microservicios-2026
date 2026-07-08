package pe.joedayz.microservicios.modulo01.solid.srp;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.solid.ocp.DiscountEngine;

/**
 * SRP (Single Responsibility): UNA sola responsabilidad -> calcular el precio final.
 *
 * <p>Anti-patron a evitar: un {@code OrderManager} que calcula precios, guarda en BD,
 * envia emails y llama a pagos. Ese "God class" tiene multiples razones para cambiar.
 * Aqui la unica razon de cambio de esta clase es la logica de precios/descuentos.
 */
public class OrderPricingService {

    private final DiscountEngine discountEngine;

    public OrderPricingService(DiscountEngine discountEngine) {
        this.discountEngine = discountEngine;
    }

    public Money finalPrice(Order order) {
        Money total = order.total();
        Money discount = discountEngine.totalDiscount(order);
        Money finalPrice = total.amount().subtract(discount.amount()).signum() < 0
                ? Money.zero(total.currency())
                : new Money(total.amount().subtract(discount.amount()), total.currency());
        return finalPrice;
    }
}
