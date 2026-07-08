package pe.joedayz.microservicios.modulo01.ddd.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object para dinero: monto + moneda.
 *
 * <p>Caracteristicas de un Value Object bien hecho:
 * <ul>
 *   <li><b>Inmutable</b>: las operaciones devuelven nuevos {@code Money}.</li>
 *   <li><b>Auto-validado</b>: no puede existir en estado invalido (moneda nula, escala mala).</li>
 *   <li><b>Igualdad por valor</b>: S/100 PEN siempre es igual a otro S/100 PEN.</li>
 *   <li><b>Sin identidad</b>: no tiene id.</li>
 * </ul>
 *
 * <p>Nunca uses {@code double} para dinero: usa {@link BigDecimal} para evitar errores
 * de redondeo binario.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount no puede ser null");
        Objects.requireNonNull(currency, "currency no puede ser null");
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency debe ser ISO-4217 de 3 letras, ej. PEN/USD");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money times(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "No se pueden operar monedas distintas: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return currency + " " + amount;
    }
}
