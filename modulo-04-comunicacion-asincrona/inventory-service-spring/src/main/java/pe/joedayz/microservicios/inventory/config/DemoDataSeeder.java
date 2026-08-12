package pe.joedayz.microservicios.inventory.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.joedayz.microservicios.inventory.domain.InventoryItem;
import pe.joedayz.microservicios.inventory.repository.InventoryItemRepository;

/**
 * Semilla de inventario físico para el demo (tenant "demo").
 */
@Configuration
public class DemoDataSeeder {

    @Bean
    public CommandLineRunner seedInventory(InventoryItemRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new InventoryItem("p1", "demo", 100));
                repository.save(new InventoryItem("p2", "demo", 50));
                repository.save(new InventoryItem("p3", "demo", 0));
            }
        };
    }
}
