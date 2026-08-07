namespace JoeDayz.Microservicios.Modulo01.Solid.Isp;

/// <summary>
/// Implementacion en memoria que cumple AMBAS interfaces finas (read + admin).
/// En produccion serian dos controllers o dos BFF inyectando solo la interfaz que necesitan.
/// </summary>
public sealed class InMemoryCatalogService : ICatalogReadApi, ICatalogAdminApi
{
    private readonly Dictionary<string, ProductAdminView> _products = new()
    {
        ["ZAP-RUN-42"] = new ProductAdminView(
            "ZAP-RUN-42", "Zapatilla Running Pro", "S/ 300.00",
            "Amortiguacion maxima para corredores", 50, true, "zapatilla-running-pro"),
        ["MEDIAS-01"] = new ProductAdminView(
            "MEDIAS-01", "Medias deportivas pack x3", "S/ 20.00",
            "Pack de 3 pares, talla unica", 200, true, "medias-deportivas"),
        ["BORRADOR-99"] = new ProductAdminView(
            "BORRADOR-99", "Producto en borrador", "S/ 0.00",
            "Aun no publicado", 0, false, string.Empty)
    };

    public IReadOnlyList<ProductSummary> ListPublished() =>
        [.. _products.Values.Where(p => p.Published).Select(p => new ProductSummary(p.Sku, p.Name, p.Price))];

    public ProductDetail GetBySku(string sku)
    {
        var product = RequirePublished(sku);
        return new ProductDetail(
            product.Sku, product.Name, product.Price, product.Description,
            $"https://cdn.joedayz.pe/img/{product.Sku}.jpg");
    }

    public IReadOnlyList<ProductAdminView> ListAll() => [.. _products.Values];

    public void Create(ProductAdminView product) => _products[product.Sku] = product;

    public void UpdateSeo(string sku, string seoSlug)
    {
        if (!_products.TryGetValue(sku, out var current))
        {
            throw new ArgumentException($"SKU no encontrado: {sku}", nameof(sku));
        }

        _products[sku] = current with { SeoSlug = seoSlug };
    }

    public void Delete(string sku) => _products.Remove(sku);

    private ProductAdminView RequirePublished(string sku) =>
        _products.TryGetValue(sku, out var product) && product.Published
            ? product
            : throw new ArgumentException($"Producto no disponible: {sku}", nameof(sku));
}

/// <summary>BFF del storefront: solo depende de <see cref="ICatalogReadApi"/>, no ve borradores.</summary>
public sealed class StorefrontBff(ICatalogReadApi catalog)
{
    public IReadOnlyList<ProductSummary> HomePage() => catalog.ListPublished();

    public ProductDetail ProductPage(string sku) => catalog.GetBySku(sku);
}

/// <summary>BFF del panel admin: solo depende de <see cref="ICatalogAdminApi"/>.</summary>
public sealed class AdminBff(ICatalogAdminApi catalog)
{
    public IReadOnlyList<ProductAdminView> Dashboard() => catalog.ListAll();

    public void PublishProduct(ProductAdminView product) => catalog.Create(product);

    public void OptimizeSeo(string sku, string slug) => catalog.UpdateSeo(sku, slug);
}
