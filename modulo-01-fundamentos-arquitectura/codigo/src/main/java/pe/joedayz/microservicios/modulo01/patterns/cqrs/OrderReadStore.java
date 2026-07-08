package pe.joedayz.microservicios.modulo01.patterns.cqrs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacen del read model. Simula la base de datos de lecturas (que suele ser distinta
 * de la de escrituras en CQRS). Clave por tenant + orderId.
 */
public class OrderReadStore {

    private final Map<String, OrderSummaryReadModel> summaries = new ConcurrentHashMap<>();

    public void put(OrderSummaryReadModel summary) {
        summaries.put(key(summary.tenantId(), summary.orderId()), summary);
    }

    public OrderSummaryReadModel get(String tenantId, String orderId) {
        return summaries.get(key(tenantId, orderId));
    }

    private String key(String tenantId, String orderId) {
        return tenantId + "::" + orderId;
    }
}
