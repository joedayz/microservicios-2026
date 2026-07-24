# 4. Feign Client (OpenFeign)

**OpenFeign** permite declarar un cliente HTTP como **interfaz Java**. Spring Cloud genera
el proxy en runtime.

## En el curso

```java
@FeignClient(name = "catalog-service", url = "${clients.catalog.url}")
public interface CatalogFeignClient {

    @GetMapping("/api/v1/products/{sku}")
    ProductDto getBySku(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("sku") String sku);
}
```

```java
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication { ... }
```

Versiones: **Spring Boot 4.0.7** + **Spring Cloud 2025.1.2** (Oakwood) → OpenFeign 5.x.

## RestClient vs Feign

| | RestClient | Feign |
|--|------------|-------|
| Estilo | Imperativo / fluent | Declarativo (interfaz) |
| Boilerplate | Medio | Bajo |
| Integración LB / Resilience4j | Manual o wrappers | Madura en Spring Cloud |
| Debugging | Muy explícito | Proxy + logs Feign |

## URL fija vs discovery

En local usamos `url=${clients.catalog.url}` (sin Eureka).

En Kubernetes (doc 5) puedes:

1. Dejar `url=http://catalog-service:8081` (DNS del Service), o
2. Usar Spring Cloud LoadBalancer + nombre lógico `catalog-service` si tienes discovery.

Para este módulo preferimos **DNS de Kubernetes** (más simple y cloud-native).

## Logging

```yaml
spring.cloud.openfeign.client.config.default.loggerLevel: basic
logging.level.pe.joedayz.microservicios.order.client: DEBUG
```

## Ejercicio

1. Añade `listProducts()` al Feign client.
2. Crea un interceptor Feign que siempre propague `X-Tenant-ID` desde `TenantContext`
   (así no lo pasas en cada método).
