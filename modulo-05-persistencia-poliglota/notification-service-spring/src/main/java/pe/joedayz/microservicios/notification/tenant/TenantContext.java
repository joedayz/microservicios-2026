package pe.joedayz.microservicios.notification.tenant;

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

    public static void clear() {
        CURRENT.remove();
    }
}
