package pe.joedayz.microservicios.catalog.config;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import pe.joedayz.microservicios.catalog.tenant.TenantKeyNormalizer;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final CatalogTenantProperties tenantProperties;

    public RateLimitService(StringRedisTemplate redisTemplate, CatalogTenantProperties tenantProperties) {
        this.redisTemplate = redisTemplate;
        this.tenantProperties = tenantProperties;
    }

    public boolean isAllowed(String tenantId, String clientId) {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        String key = "catalog:rate-limit:%s:%s:%d".formatted(
                TenantKeyNormalizer.normalize(tenantId),
                clientId,
                currentMinute);

        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == null) {
            throw new IllegalStateException("No se pudo registrar el rate limit en Redis");
        }
        if (currentCount == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }
        return currentCount <= tenantProperties.getRateLimit().getMaxRequestsPerMinute();
    }
}
