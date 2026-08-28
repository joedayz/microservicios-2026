package pe.joedayz.microservicios.security.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import pe.joedayz.microservicios.security.inventory.api.InventoryItemResponse;

@ApplicationScoped
public class InventoryService {

    private final Map<String, InventoryItemResponse> items = new ConcurrentHashMap<>(Map.of(
            key("tienda-deportes", "ZAP-RUN-42"),
            new InventoryItemResponse("tienda-deportes", "ZAP-RUN-42", 25, 3, "PE"),
            key("tienda-deportes", "POL-TRK-09"),
            new InventoryItemResponse("tienda-deportes", "POL-TRK-09", 12, 0, "PE"),
            key("libreria-lima", "LIB-DDD-01"),
            new InventoryItemResponse("libreria-lima", "LIB-DDD-01", 15, 1, "PE")));

    public List<InventoryItemResponse> listItems(String tenantId) {
        return items.values().stream()
                .filter(item -> item.tenantId().equalsIgnoreCase(tenantId))
                .sorted((a, b) -> a.sku().compareTo(b.sku()))
                .toList();
    }

    public InventoryItemResponse getItem(String tenantId, String sku) {
        InventoryItemResponse item = items.get(key(tenantId, sku));
        if (item == null) {
            throw new NoSuchElementException("SKU no encontrado para el tenant: " + sku);
        }
        return item;
    }

    public InventoryItemResponse reserve(String tenantId, String sku, int quantity) {
        return items.compute(key(tenantId, sku), (itemKey, current) -> {
            if (current == null) {
                throw new NoSuchElementException("SKU no encontrado para el tenant: " + sku);
            }
            if (current.availableQuantity() < quantity) {
                throw new IllegalArgumentException("Stock insuficiente para " + sku);
            }
            return new InventoryItemResponse(
                    current.tenantId(),
                    current.sku(),
                    current.availableQuantity() - quantity,
                    current.reservedQuantity() + quantity,
                    current.warehouseRegion());
        });
    }

    private static String key(String tenantId, String sku) {
        return tenantId.toLowerCase() + "::" + sku.toUpperCase();
    }
}
