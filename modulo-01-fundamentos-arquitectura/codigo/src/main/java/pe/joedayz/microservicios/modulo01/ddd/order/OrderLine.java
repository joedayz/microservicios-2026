package pe.joedayz.microservicios.modulo01.ddd.order;

import pe.joedayz.microservicios.modulo01.ddd.shared.Money;
import pe.joedayz.microservicios.modulo01.ddd.shared.Quantity;
import pe.joedayz.microservicios.modulo01.ddd.shared.Sku;

/**
 * Linea de un pedido. Vive DENTRO del agregado {@link Order} y no se manipula por
 * fuera: se crea a traves de {@code order.addLine(...)}, que valida los invariantes.
 *
 * <p>Es inmutable; representa "N unidades del SKU X a un precio unitario".
 */
public record OrderLine(Sku sku, Quantity quantity, Money unitPrice) {

    public Money subtotal() {
        return unitPrice.times(quantity.value());
    }
}
