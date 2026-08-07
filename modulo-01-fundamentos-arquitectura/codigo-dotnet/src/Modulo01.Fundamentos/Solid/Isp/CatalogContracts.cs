namespace JoeDayz.Microservicios.Modulo01.Solid.Isp;

/// <summary>Vista liviana para storefront / app movil: solo lo que se ve en un listado.</summary>
public sealed record ProductSummary(string Sku, string Name, string Price);

/// <summary>Vista de detalle para la web: mas campos, pero sin operaciones de admin.</summary>
public sealed record ProductDetail(string Sku, string Name, string Price, string Description, string ImageUrl);

/// <summary>Vista completa del back-office: incluye campos internos que el cliente nunca debe ver.</summary>
public sealed record ProductAdminView(
    string Sku,
    string Name,
    string Price,
    string Description,
    int Stock,
    bool Published,
    string SeoSlug);

/// <summary>ISP: interfaz FINA para consumidores de lectura (storefront, BFF movil, BFF web).</summary>
public interface ICatalogReadApi
{
    IReadOnlyList<ProductSummary> ListPublished();

    ProductDetail GetBySku(string sku);
}

/// <summary>ISP: interfaz FINA para el back-office (BFF admin / panel interno).</summary>
public interface ICatalogAdminApi
{
    IReadOnlyList<ProductAdminView> ListAll();

    void Create(ProductAdminView product);

    void UpdateSeo(string sku, string seoSlug);

    void Delete(string sku);
}

/// <summary>
/// ANTI-PATRON (viola ISP): una "God API" que mezcla storefront y administracion.
/// La app movil terminaria dependiendo de metodos que nunca usa.
/// </summary>
public interface ICatalogGodApi
{
    IReadOnlyList<ProductSummary> ListPublished();

    ProductDetail GetBySku(string sku);

    IReadOnlyList<ProductAdminView> ListAllIncludingDrafts();

    void CreateProduct(ProductAdminView product);

    void UpdateSeo(string sku, string seoSlug);

    void DeleteProduct(string sku);
}
