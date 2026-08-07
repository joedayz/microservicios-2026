namespace JoeDayz.Microservicios.Modulo01.Solid.Dip;

/// <summary>
/// Demo minima de DIP: <see cref="OrderPersistenceService"/> depende de la interfaz,
/// asi que cambiar de SQL Server a PostgreSQL es cambiar una linea del composition root
/// (en ASP.NET Core, una linea en <c>builder.Services</c>).
/// </summary>
public interface IOrderStore
{
    void SaveOrder(string orderId);
}

public sealed class SqlServerOrderStore : IOrderStore
{
    public void SaveOrder(string orderId) => Console.WriteLine($"   [DIP] Guardando orden {orderId} en SQL Server");
}

public sealed class PostgreSqlOrderStore : IOrderStore
{
    public void SaveOrder(string orderId) => Console.WriteLine($"   [DIP] Guardando orden {orderId} en PostgreSQL");
}

public sealed class OrderPersistenceService(IOrderStore store)
{
    public void ProcessOrder(string orderId)
    {
        Console.WriteLine("   [DIP] Procesando orden...");
        store.SaveOrder(orderId);
    }
}
