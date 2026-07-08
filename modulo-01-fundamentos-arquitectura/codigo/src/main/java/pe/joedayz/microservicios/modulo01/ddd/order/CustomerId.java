package pe.joedayz.microservicios.modulo01.ddd.order;

import java.util.Objects;

/**
 * Identidad del cliente. En DDD, {@code Order} referencia al cliente por su ID
 * (no por el objeto Customer completo), porque Customer es OTRO agregado.
 * Regla: los agregados se referencian entre si por identidad, nunca por referencia directa.
 */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "customerId no puede ser null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("customerId no puede estar vacio");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
