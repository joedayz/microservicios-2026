package pe.joedayz.microservicios.modulo01.solid.ocp;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;
import pe.joedayz.microservicios.modulo01.ddd.shared.Money;

/**
 * OCP (Open/Closed): una regla de descuento.
 *
 * <p>Para agregar una nueva politica de descuento se crea una nueva implementacion
 * de esta interfaz; el {@link DiscountEngine} NO se modifica (cerrado a modificacion,
 * abierto a extension). En microservicios el equivalente es suscribir un nuevo
 * consumidor a un evento sin tocar el productor.
 */
public interface DiscountRule {

    boolean applies(Order order);

    Money discountFor(Order order);

    String name();
}
