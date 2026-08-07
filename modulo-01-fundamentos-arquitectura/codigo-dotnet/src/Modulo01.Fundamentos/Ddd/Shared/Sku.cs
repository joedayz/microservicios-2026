using System.Text.RegularExpressions;

namespace JoeDayz.Microservicios.Modulo01.Ddd.Shared;

/// <summary>
/// Codigo de producto (Stock Keeping Unit) con formato validado.
/// Usa un regex generado en tiempo de compilacion (source generator de .NET),
/// asi no hay coste de interpretacion en runtime ni reflexion en AOT.
/// </summary>
public sealed partial record Sku
{
    public Sku(string value)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(value, nameof(value));
        if (!SkuPattern().IsMatch(value))
        {
            throw new ArgumentException(
                $"SKU invalido: '{value}'. Debe ser 3-20 caracteres A-Z, 0-9 o '-'", nameof(value));
        }

        Value = value;
    }

    public string Value { get; }

    public static Sku Of(string value) => new(value);

    public override string ToString() => Value;

    [GeneratedRegex("^[A-Z0-9\\-]{3,20}$")]
    private static partial Regex SkuPattern();
}
