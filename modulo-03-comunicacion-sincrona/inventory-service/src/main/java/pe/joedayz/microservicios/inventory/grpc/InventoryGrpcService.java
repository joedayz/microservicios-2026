package pe.joedayz.microservicios.inventory.grpc;

import pe.joedayz.microservicios.inventory.domain.StockStore;
import pe.joedayz.microservicios.inventory.v1.CheckStockRequest;
import pe.joedayz.microservicios.inventory.v1.CheckStockResponse;
import pe.joedayz.microservicios.inventory.v1.GetAvailabilityRequest;
import pe.joedayz.microservicios.inventory.v1.GetAvailabilityResponse;
import pe.joedayz.microservicios.inventory.v1.InventoryServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final StockStore stockStore;

    public InventoryGrpcService(StockStore stockStore) {
        this.stockStore = stockStore;
    }

    @Override
    public void checkStock(CheckStockRequest request, StreamObserver<CheckStockResponse> responseObserver) {
        int onHand = stockStore.get(request.getTenantId(), request.getSku()).orElse(0);
        boolean available = onHand >= request.getQuantity();
        int remaining = available ? onHand - request.getQuantity() : onHand;

        CheckStockResponse response = CheckStockResponse.newBuilder()
                .setAvailable(available)
                .setRemaining(remaining)
                .setMessage(available
                        ? "Stock suficiente"
                        : "Stock insuficiente (disponible=" + onHand + ")")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAvailability(GetAvailabilityRequest request,
                                StreamObserver<GetAvailabilityResponse> responseObserver) {
        int onHand = stockStore.get(request.getTenantId(), request.getSku()).orElse(0);

        GetAvailabilityResponse response = GetAvailabilityResponse.newBuilder()
                .setSku(request.getSku())
                .setQuantityOnHand(onHand)
                .setWarehouse("LIM-01")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
