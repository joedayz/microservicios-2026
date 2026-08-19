package pe.joedayz.microservicios.catalog.config;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayMigrationConfig {

    @Bean
    public TenantFlywayMigration tenantFlywayMigration(Map<String, DataSource> tenantDataSources) {
        tenantDataSources.forEach((tenant, dataSource) -> Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate());
        return new TenantFlywayMigration();
    }

    public static final class TenantFlywayMigration {

        private TenantFlywayMigration() {
        }
    }
}
