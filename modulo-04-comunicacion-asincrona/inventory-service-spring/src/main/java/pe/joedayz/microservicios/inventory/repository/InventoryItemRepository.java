package pe.joedayz.microservicios.inventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.joedayz.microservicios.inventory.domain.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    Optional<InventoryItem> findBySkuAndTenantId(String sku, String tenantId);
}
