package pe.joedayz.microservicios.modulo01.solid.dip;

import pe.joedayz.microservicios.modulo01.ddd.shared.Quantity;
import pe.joedayz.microservicios.modulo01.ddd.shared.Sku;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADAPTADOR en memoria del {@link InventoryPort}. En produccion existiria un
 * {@code RestInventoryAdapter} o {@code GrpcInventoryAdapter} implementando el mismo puerto.
 *
 * <p>El stock se almacena por clave (tenant + sku), reforzando el aislamiento multi-tenant.
 */
public class InMemoryInventoryAdapter implements InventoryPort {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    public void setStock(TenantId tenantId, Sku sku, int units) {
        stock.put(key(tenantId, sku), units);
    }

    public int available(TenantId tenantId, Sku sku) {
        return stock.getOrDefault(key(tenantId, sku), 0);
    }

    @Override
    public boolean reserve(TenantId tenantId, Sku sku, Quantity quantity) {
        String key = key(tenantId, sku);
        Integer current = stock.getOrDefault(key, 0);
        if (current < quantity.value()) {
            return false;
        }
        stock.put(key, current - quantity.value());
        return true;
    }

    @Override
    public void release(TenantId tenantId, Sku sku, Quantity quantity) {
        String key = key(tenantId, sku);
        stock.merge(key, quantity.value(), Integer::sum);
    }

    private String key(TenantId tenantId, Sku sku) {
        return tenantId.value() + "::" + sku.value();
    }
}
