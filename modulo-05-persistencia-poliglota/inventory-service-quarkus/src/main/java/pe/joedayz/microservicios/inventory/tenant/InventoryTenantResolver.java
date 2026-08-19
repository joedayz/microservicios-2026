package pe.joedayz.microservicios.inventory.tenant;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

@RequestScoped
@PersistenceUnitExtension
public class InventoryTenantResolver implements TenantResolver {

    @Override
    public String getDefaultTenantId() {
        return "tienda_deportes";
    }

    @Override
    public String resolveTenantId() {
        return TenantContext.requireKey();
    }
}
