namespace JoeDayz.Microservicios.Modulo01.Tenant;

/// <summary>
/// Identificador de tenant. Value object: sin identidad propia, se compara por valor.
/// En un e-commerce multi-tenant TODA operacion viaja acompaniada de un TenantId.
/// </summary>
public sealed record TenantId
{
    public TenantId(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value, nameof(value));
        Value = value;
    }

    public string Value { get; }

    public static TenantId Of(string value) => new(value);

    public override string ToString() => Value;
}
