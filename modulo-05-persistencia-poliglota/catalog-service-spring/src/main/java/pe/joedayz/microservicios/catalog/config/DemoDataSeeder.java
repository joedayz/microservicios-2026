package pe.joedayz.microservicios.catalog.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DemoDataSeeder {

    @Bean
    public ApplicationRunner seedCatalogData(Map<String, DataSource> tenantDataSources) {
        return args -> tenantDataSources.forEach((tenant, dataSource) -> seedTenant(tenant, new JdbcTemplate(dataSource)));
    }

    private void seedTenant(String tenant, JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        for (ProductSeed seed : seedsFor(tenant)) {
            jdbcTemplate.update("""
                    INSERT INTO products (sku, name, description, category, price, currency)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    seed.sku(),
                    seed.name(),
                    seed.description(),
                    seed.category(),
                    seed.price(),
                    seed.currency());
        }
    }

    private List<ProductSeed> seedsFor(String tenant) {
        return switch (tenant) {
            case "tienda_deportes" -> List.of(
                    new ProductSeed("ZAP-RUN-42", "Zapatilla Running Pro", "Amortiguacion maxima para corredores", "calzado",
                            new BigDecimal("300.00"), "PEN"),
                    new ProductSeed("BAL-FUT-01", "Balon Match Pro", "Balon oficial para entrenamiento y competencia", "deportes",
                            new BigDecimal("120.00"), "PEN"));
            case "libreria_lima" -> List.of(
                    new ProductSeed("LIB-DDD-01", "Domain-Driven Design", "Eric Evans - libro imprescindible", "libros",
                            new BigDecimal("120.00"), "PEN"),
                    new ProductSeed("JAVA-25-01", "Java 25 Developer Guide", "Guia practica Java moderno", "libros",
                            new BigDecimal("95.00"), "PEN"));
            default -> List.of();
        };
    }

    private record ProductSeed(String sku,
                               String name,
                               String description,
                               String category,
                               BigDecimal price,
                               String currency) {
    }
}
