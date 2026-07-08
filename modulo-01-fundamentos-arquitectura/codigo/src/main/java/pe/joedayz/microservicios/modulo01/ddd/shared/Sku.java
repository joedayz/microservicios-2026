package pe.joedayz.microservicios.modulo01.ddd.shared;

import java.util.Objects;

/**
 * Value Object: SKU (Stock Keeping Unit), el codigo unico de un producto.
 *
 * <p>En el contexto de Catalogo/Inventario un producto se identifica por su SKU.
 * Se auto-valida el formato para que nunca exista un SKU malformado en el sistema.
 */
public record Sku(String value) {

    public Sku {
        Objects.requireNonNull(value, "sku no puede ser null");
        if (!value.matches("[A-Z0-9\\-]{3,20}")) {
            throw new IllegalArgumentException(
                    "SKU invalido: '" + value + "'. Debe ser 3-20 caracteres A-Z, 0-9 o '-'");
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
