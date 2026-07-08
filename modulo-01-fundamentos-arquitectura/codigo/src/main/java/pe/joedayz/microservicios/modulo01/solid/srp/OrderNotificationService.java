package pe.joedayz.microservicios.modulo01.solid.srp;

import pe.joedayz.microservicios.modulo01.ddd.order.Order;

/**
 * SRP: responsabilidad unica -> notificar. Separada de {@link OrderPricingService}.
 *
 * <p>En el sistema real esto seria el microservicio {@code Notification}, desacoplado
 * por eventos: reacciona a OrderConfirmed sin que el servicio de pedidos sepa de emails.
 */
public class OrderNotificationService {

    public void notifyConfirmed(Order order) {
        System.out.printf("   [Notification] Email al cliente %s: tu pedido %s fue confirmado.%n",
                order.customerId(), order.id());
    }
}
