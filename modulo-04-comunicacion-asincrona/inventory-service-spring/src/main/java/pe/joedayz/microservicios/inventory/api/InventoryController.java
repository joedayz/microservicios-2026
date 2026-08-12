package pe.joedayz.microservicios.inventory.api;

import java.util.NoSuchElementException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.inventory.api.dto.InventoryResponse;
import pe.joedayz.microservicios.inventory.repository.InventoryItemRepository;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryController(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @GetMapping("/{sku}")
    public InventoryResponse getInventory(@PathVariable String sku, @RequestHeader("X-Tenant-ID") String tenantId) {
        return inventoryItemRepository.findBySkuAndTenantId(sku, tenantId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new NoSuchElementException("SKU no encontrado: " + sku));
    }
}
