package pe.joedayz.microservicios.catalog.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import pe.joedayz.microservicios.catalog.tenant.TenantKeyNormalizer;

@ConfigurationProperties(prefix = "catalog")
public class CatalogTenantProperties {

    private Map<String, TenantDataSourceProperties> tenants = new LinkedHashMap<>();
    private RateLimitProperties rateLimit = new RateLimitProperties();

    public Map<String, TenantDataSourceProperties> getTenants() {
        return tenants;
    }

    public void setTenants(Map<String, TenantDataSourceProperties> tenants) {
        this.tenants = tenants;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public TenantDataSourceProperties requireTenant(String tenantId) {
        TenantDataSourceProperties properties = tenants.get(TenantKeyNormalizer.normalize(tenantId));
        if (properties == null) {
            throw new IllegalArgumentException("Tenant no configurado: " + tenantId);
        }
        return properties;
    }

    public static class TenantDataSourceProperties {

        private String jdbcUrl;
        private String username;
        private String password;

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RateLimitProperties {

        private int maxRequestsPerMinute = 30;

        public int getMaxRequestsPerMinute() {
            return maxRequestsPerMinute;
        }

        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }
    }
}
