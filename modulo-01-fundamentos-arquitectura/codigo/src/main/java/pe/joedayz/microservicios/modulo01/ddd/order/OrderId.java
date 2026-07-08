package pe.joedayz.microservicios.modulo01.ddd.order;

import java.util.UUID;

/**
 * Identidad de la entidad Order. A diferencia de un Value Object, define IDENTIDAD:
 * dos pedidos con los mismos datos pero distinto OrderId son pedidos distintos.
 */
public record OrderId(UUID value) {

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
