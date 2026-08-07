namespace JoeDayz.Microservicios.Modulo01.Ddd.Shared;

/// <summary>
/// Value object de dinero. Inmutable, se compara por valor y protege sus invariantes:
/// moneda ISO-4217 de 3 letras y redondeo bancario a 2 decimales.
/// En .NET usamos <see cref="decimal"/> (equivalente a BigDecimal para importes).
/// </summary>
public sealed record Money
{
    public Money(decimal amount, string currency)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(currency, nameof(currency));
        if (currency.Length != 3)
        {
            throw new ArgumentException("currency debe ser ISO-4217 de 3 letras, ej. PEN/USD", nameof(currency));
        }

        Amount = Math.Round(amount, 2, MidpointRounding.ToEven);
        Currency = currency;
    }

    public decimal Amount { get; }

    public string Currency { get; }

    public static Money Of(decimal amount, string currency) => new(amount, currency);

    public static Money Zero(string currency) => new(0m, currency);

    public Money Plus(Money other)
    {
        RequireSameCurrency(other);
        return new Money(Amount + other.Amount, Currency);
    }

    public Money Minus(Money other)
    {
        RequireSameCurrency(other);
        return new Money(Amount - other.Amount, Currency);
    }

    public Money Times(int factor) => new(Amount * factor, Currency);

    public bool IsGreaterThan(Money other)
    {
        RequireSameCurrency(other);
        return Amount > other.Amount;
    }

    public bool IsNegative => Amount < 0m;

    private void RequireSameCurrency(Money other)
    {
        if (Currency != other.Currency)
        {
            throw new ArgumentException(
                $"No se pueden operar monedas distintas: {Currency} vs {other.Currency}", nameof(other));
        }
    }

    public override string ToString() => $"{Currency} {Amount:0.00}";
}
