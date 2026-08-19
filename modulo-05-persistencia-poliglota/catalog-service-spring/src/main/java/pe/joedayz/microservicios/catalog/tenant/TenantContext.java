package pe.joedayz.microservicios.catalog.tenant;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String require() {
        String tenant = CURRENT.get();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("Header X-Tenant-ID requerido");
        }
        return tenant;
    }

    public static String requireKey() {
        return TenantKeyNormalizer.normalize(require());
    }

    public static String getOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
