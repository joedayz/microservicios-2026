namespace JoeDayz.Microservicios.Modulo01.Ddd.Shared;

/// <summary>
/// Cantidad siempre positiva. Un value object evita que un <c>int</c> suelto
/// se cuele con valores imposibles (0 o negativos) en el dominio.
/// </summary>
public readonly record struct Quantity
{
    public Quantity(int value)
    {
        if (value <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(value), value, "La cantidad debe ser mayor a 0");
        }

        Value = value;
    }

    public int Value { get; }

    public static Quantity Of(int value) => new(value);

    public override string ToString() => Value.ToString();
}
