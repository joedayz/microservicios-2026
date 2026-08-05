package pe.joedayz.microservicios.catalog.repository;

import pe.joedayz.microservicios.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByTenantId(String tenantId);

    Optional<Product> findByTenantIdAndSku(String tenantId, String sku);
}
