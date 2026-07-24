package pe.joedayz.microservicios.order.config;

import pe.joedayz.microservicios.inventory.v1.InventoryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientsConfig {

    @Bean
    InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub(GrpcChannelFactory channels) {
        return InventoryServiceGrpc.newBlockingStub(channels.createChannel("inventory"));
    }
}
