package pe.joedayz.microservicios.modulo01.ddd.shared;

/**
 * Value Object: cantidad de unidades. Siempre positiva (invariante del dominio).
 */
public record Quantity(int value) {

    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0, recibido: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
