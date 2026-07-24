package pe.joedayz.microservicios.catalog.config;

import pe.joedayz.microservicios.catalog.domain.Product;
import pe.joedayz.microservicios.catalog.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String TENANT = "tienda-deportes";

    private final ProductRepository repository;

    public DataInitializer(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        repository.saveAll(java.util.List.of(
                new Product(TENANT, "ZAP-RUN-42", "Zapatilla Running Pro",
                        "Amortiguacion maxima para corredores", new BigDecimal("300.00"), "PEN"),
                new Product(TENANT, "CAM-DRY-M", "Camiseta Dry Fit M",
                        "Tejido transpirable para entrenamiento", new BigDecimal("45.00"), "PEN"),
                new Product(TENANT, "MEDIAS-01", "Medias deportivas pack x3",
                        "Pack de 3 pares", new BigDecimal("20.00"), "PEN")
        ));
    }
}
