package pe.joedayz.microservicios.catalog.repository;

import pe.joedayz.microservicios.catalog.domain.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductR2dbcRepository extends ReactiveCrudRepository<Product, String> {

    Flux<Product> findByTenantId(String tenantId);

    Mono<Product> findByTenantIdAndSku(String tenantId, String sku);
}
