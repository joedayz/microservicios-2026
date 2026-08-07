namespace JoeDayz.Microservicios.Modulo01.Ddd.Orders;

/// <summary>Referencia al cliente. Vive en otro bounded context (Customers): solo guardamos su id.</summary>
public sealed record CustomerId
{
    public CustomerId(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value, nameof(value));
        Value = value;
    }

    public string Value { get; }

    public static CustomerId Of(string value) => new(value);

    public override string ToString() => Value;
}
