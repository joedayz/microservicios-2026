package pe.joedayz.microservicios.modulo01.tenant;

import java.util.Objects;

/**
 * Identificador de un tenant (tienda) en la plataforma SaaS multi-tenant.
 *
 * <p>Es un Value Object: inmutable y se compara por valor. Todo dato de negocio
 * (pedidos, catalogo, stock) pertenece a un tenant y jamas se cruza entre tenants.
 */
public record TenantId(String value) {

    public TenantId {
        Objects.requireNonNull(value, "tenantId no puede ser null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("tenantId no puede estar vacio");
        }
    }

    public static TenantId of(String value) {
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
