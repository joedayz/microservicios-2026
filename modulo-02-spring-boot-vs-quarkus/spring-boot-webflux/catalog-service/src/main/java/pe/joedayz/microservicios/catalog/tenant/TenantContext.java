package pe.joedayz.microservicios.catalog.tenant;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public final class TenantContext {

    public static final String TENANT_KEY = "tenantId";

    private TenantContext() {
    }

    public static Context withTenant(String tenantId) {
        return Context.of(TENANT_KEY, tenantId);
    }

    public static Mono<String> require() {
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(TENANT_KEY)) {
                return Mono.error(new IllegalStateException("Header X-Tenant-ID requerido"));
            }
            String tenant = ctx.get(TENANT_KEY);
            if (tenant == null || tenant.isBlank()) {
                return Mono.error(new IllegalStateException("Header X-Tenant-ID requerido"));
            }
            return Mono.just(tenant);
        });
    }
}
