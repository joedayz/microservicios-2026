package pe.joedayz.microservicios.resilience.order.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import pe.joedayz.microservicios.resilience.order.api.OrderRequest;
import pe.joedayz.microservicios.resilience.order.api.OrderResponse;
import pe.joedayz.microservicios.resilience.order.client.InventoryClient;
import pe.joedayz.microservicios.resilience.order.client.RiskClient;

@Service
public class OrderService {

    private final InventoryClient inventoryClient;
    private final RiskClient riskClient;

    public OrderService(InventoryClient inventoryClient, RiskClient riskClient) {
        this.inventoryClient = inventoryClient;
        this.riskClient = riskClient;
    }

    public OrderResponse place(OrderRequest req) {
        CompletableFuture<Map<String, Object>> inv =
                inventoryClient.reserve(req.sku(), req.qty(), req.orderId());
        CompletableFuture<Map<String, Object>> risk =
                riskClient.score(req.orderId());

        Map<String, Object> inventory = inv.join();
        Map<String, Object> riskResult = risk.join();

        boolean invDegraded = "DEGRADED".equals(inventory.get("status"));
        boolean riskDegraded = "MANUAL_REVIEW".equals(riskResult.get("decision"));
        String degradation = switch ((invDegraded ? 1 : 0) + (riskDegraded ? 2 : 0)) {
            case 0 -> "NONE";
            case 1 -> "INVENTORY_FALLBACK";
            case 2 -> "RISK_FALLBACK";
            default -> "BOTH_FALLBACK";
        };
        String status = invDegraded ? "PENDING" : "CONFIRMED";

        return new OrderResponse(req.orderId(), req.sku(), req.qty(),
                status, inventory, riskResult, degradation);
    }
}

