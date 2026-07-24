package pe.joedayz.microservicios.inventory.domain;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockStore {

    private final Map<StockKey, Integer> stock = new ConcurrentHashMap<>();

    public StockStore() {
        put("tienda-deportes", "ZAP-RUN-42", 50);
        put("tienda-deportes", "CAM-DRY-M", 20);
        put("tienda-deportes", "MEDIAS-01", 100);
    }

    public void put(String tenantId, String sku, int quantity) {
        stock.put(new StockKey(tenantId, sku), quantity);
    }

    public Optional<Integer> get(String tenantId, String sku) {
        return Optional.ofNullable(stock.get(new StockKey(tenantId, sku)));
    }
}
