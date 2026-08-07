namespace JoeDayz.Microservicios.Modulo01.Tenant;

/// <summary>
/// Contexto de tenant de la peticion actual.
/// Equivalente .NET del <c>ThreadLocal</c> de Java: <see cref="AsyncLocal{T}"/> fluye
/// a traves de <c>await</c>, por lo que funciona igual con async/await y con Task.Run.
/// En ASP.NET Core esto se poblaria en un middleware a partir del subdominio o del JWT.
/// </summary>
public static class TenantContext
{
    private static readonly AsyncLocal<TenantId?> Current = new();

    public static void Set(TenantId tenantId) => Current.Value = tenantId;

    public static TenantId Require() =>
        Current.Value
        ?? throw new InvalidOperationException(
            "No hay tenant en el contexto: toda operacion multi-tenant requiere un TenantId");

    public static void Clear() => Current.Value = null;
}
