package pe.joedayz.microservicios.catalog.service;

import pe.joedayz.microservicios.catalog.tenant.TenantContext;

public final class ProductCacheKeys {

    private ProductCacheKeys() {
    }

    public static String listKey() {
        return TenantContext.requireKey() + ":list";
    }

    public static String skuKey(String sku) {
        return TenantContext.requireKey() + ":sku:" + sku;
    }
}
