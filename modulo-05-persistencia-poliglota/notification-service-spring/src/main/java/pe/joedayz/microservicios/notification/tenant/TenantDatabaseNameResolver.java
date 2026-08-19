package pe.joedayz.microservicios.notification.tenant;

import java.util.Locale;

public final class TenantDatabaseNameResolver {

    private TenantDatabaseNameResolver() {
    }

    public static String normalize(String tenantId) {
        return tenantId.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "_");
    }
}
