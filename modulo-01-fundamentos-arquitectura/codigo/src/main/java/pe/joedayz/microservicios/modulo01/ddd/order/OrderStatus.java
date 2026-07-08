package pe.joedayz.microservicios.modulo01.ddd.order;

/**
 * Ciclo de vida del pedido. El agregado {@link Order} controla las transiciones
 * validas entre estos estados (es un invariante: no se puede pasar de CONFIRMED a DRAFT).
 *
 * <pre>
 *   DRAFT ──place()──► PLACED ──confirm()──► CONFIRMED
 *                        │
 *                        └──cancel()──► CANCELLED
 * </pre>
 */
public enum OrderStatus {
    DRAFT,
    PLACED,
    CONFIRMED,
    CANCELLED
}
