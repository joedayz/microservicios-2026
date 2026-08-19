package pe.joedayz.microservicios.catalog.tenant;

import java.util.Locale;

public final class TenantKeyNormalizer {

    private TenantKeyNormalizer() {
    }

    public static String normalize(String tenantId) {
        return tenantId.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "_");
    }
}
