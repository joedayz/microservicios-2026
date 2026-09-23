package pe.joedayz.microservicios.observability.order.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import pe.joedayz.microservicios.observability.order.api.OrderRequest;
import pe.joedayz.microservicios.observability.order.api.OrderResponse;
import pe.joedayz.microservicios.observability.order.client.InventoryClient;
import pe.joedayz.microservicios.observability.order.client.RiskClient;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final InventoryClient inventoryClient;
    private final RiskClient riskClient;
    private final ObservationRegistry observations;
    private final MeterRegistry meters;

    public OrderService(InventoryClient inventoryClient, RiskClient riskClient,
            ObservationRegistry observations, MeterRegistry meters) {
        this.inventoryClient = inventoryClient;
        this.riskClient = riskClient;
        this.observations = observations;
        this.meters = meters;
    }

    public OrderResponse place(OrderRequest req) {
        String tenant = tenant();
        return Observation.createNotStarted("order.place", observations)
                .highCardinalityKeyValue("order.id", req.orderId())
                .highCardinalityKeyValue("sku", req.sku())
                .highCardinalityKeyValue("tenant.id", tenant)
                .observe(() -> doPlace(req, tenant));
    }

    private OrderResponse doPlace(OrderRequest req, String tenant) {
        CompletableFuture<Map<String, Object>> inv =
                inventoryClient.reserve(req.sku(), req.qty(), req.orderId());
        CompletableFuture<Map<String, Object>> risk = riskClient.score(req.orderId());

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

        // status y degradation son conjuntos chicos: sirven como labels.
        // tenant.id y order.id no: viven en la traza y en el log, no en Prometheus.
        Counter.builder("orders.placed")
                .tag("status", status)
                .tag("degradation", degradation)
                .register(meters)
                .increment();

        log.info("orden colocada orderId={} sku={} qty={} status={} degradation={}",
                req.orderId(), req.sku(), req.qty(), status, degradation);

        return new OrderResponse(req.orderId(), req.sku(), req.qty(),
                status, inventory, riskResult, degradation, tenant, traceId());
    }

    private String tenant() {
        String value = MDC.get("tenant.id");
        return (value == null || value.isBlank()) ? "tienda-deportes" : value;
    }

    private String traceId() {
        String value = MDC.get("traceId");
        return (value == null || value.isBlank()) ? "none" : value;
    }
}
