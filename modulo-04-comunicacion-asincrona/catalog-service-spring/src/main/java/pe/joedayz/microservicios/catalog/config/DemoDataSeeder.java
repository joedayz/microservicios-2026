package pe.joedayz.microservicios.catalog.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.joedayz.microservicios.catalog.domain.ProductStock;
import pe.joedayz.microservicios.catalog.repository.ProductStockRepository;

/**
 * Semilla de stock para el demo (tenant "demo"). En producción esto vendría
 * de una migración de BD o de un proceso de carga de catálogo.
 */
@Configuration
public class DemoDataSeeder {

    @Bean
    public CommandLineRunner seedProductStock(ProductStockRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new ProductStock("p1", "demo", 100));
                repository.save(new ProductStock("p2", "demo", 50));
                repository.save(new ProductStock("p3", "demo", 0));
            }
        };
    }
}
