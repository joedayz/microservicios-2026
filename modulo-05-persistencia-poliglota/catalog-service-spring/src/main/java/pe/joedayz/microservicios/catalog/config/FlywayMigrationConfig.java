package pe.joedayz.microservicios.catalog.config;

import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayMigrationConfig {

    @Bean
    public ApplicationRunner flywayRunner(Map<String, DataSource> tenantDataSources) {
        return args -> tenantDataSources.forEach((tenant, dataSource) -> Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate());
    }
}
