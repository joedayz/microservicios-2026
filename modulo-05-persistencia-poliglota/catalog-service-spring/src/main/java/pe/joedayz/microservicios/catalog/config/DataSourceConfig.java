package pe.joedayz.microservicios.catalog.config;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import pe.joedayz.microservicios.catalog.tenant.TenantContext;
import pe.joedayz.microservicios.catalog.tenant.TenantKeyNormalizer;

@Configuration
@EnableConfigurationProperties(CatalogTenantProperties.class)
public class DataSourceConfig {

    @Bean
    public Map<String, DataSource> tenantDataSources(CatalogTenantProperties properties) {
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        properties.getTenants().forEach((tenant, config) -> dataSources.put(tenant, buildDataSource(config)));
        return dataSources;
    }

    @Bean
    @Primary
    public DataSource dataSource(Map<String, DataSource> tenantDataSources) {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();
        Map<Object, Object> targets = new LinkedHashMap<>(tenantDataSources);
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(tenantDataSources.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Debe existir al menos un datasource tenant")));
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    private DataSource buildDataSource(CatalogTenantProperties.TenantDataSourceProperties properties) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(properties.getJdbcUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
    }

    private static final class TenantRoutingDataSource extends AbstractRoutingDataSource {

        @Override
        protected Object determineCurrentLookupKey() {
            String tenant = TenantContext.getOrNull();
            return tenant == null ? null : TenantKeyNormalizer.normalize(tenant);
        }
    }
}
