package pe.joedayz.microservicios.inventory.tenant;

import java.util.Locale;

public final class TenantNormalizer {

    private TenantNormalizer() {
    }

    public static String normalize(String tenantId) {
        return tenantId.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "_");
    }
}
