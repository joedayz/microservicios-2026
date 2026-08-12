package pe.joedayz.microservicios.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import pe.joedayz.microservicios.catalog.domain.ProductStock;

public interface ProductStockRepository extends JpaRepository<ProductStock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductStock> findBySkuAndTenantId(String sku, String tenantId);
}
