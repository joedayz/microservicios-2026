package pe.joedayz.microservicios.order.client;

import pe.joedayz.microservicios.inventory.v1.CheckStockRequest;
import pe.joedayz.microservicios.inventory.v1.CheckStockResponse;
import pe.joedayz.microservicios.inventory.v1.InventoryServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class InventoryGrpcClient {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub stub;

    public InventoryGrpcClient(InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub) {
        this.stub = inventoryStub;
    }

    public CheckStockResponse checkStock(String tenantId, String sku, int quantity) {
        return stub.checkStock(CheckStockRequest.newBuilder()
                .setTenantId(tenantId)
                .setSku(sku)
                .setQuantity(quantity)
                .build());
    }
}
