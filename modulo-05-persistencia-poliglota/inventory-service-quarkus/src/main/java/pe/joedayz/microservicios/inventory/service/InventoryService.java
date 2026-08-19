package pe.joedayz.microservicios.inventory.service;

import java.util.List;

import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import pe.joedayz.microservicios.inventory.api.InventoryResponse;
import pe.joedayz.microservicios.inventory.domain.InventoryItem;

@ApplicationScoped
public class InventoryService {

    public List<InventoryResponse> listItems() {
        return InventoryItem.listAllItems().stream()
                .map(InventoryResponse::from)
                .toList();
    }

    public InventoryResponse getItem(String sku) {
        return InventoryItem.findBySku(sku)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new NotFoundException("SKU no encontrado: " + sku));
    }

    @Transactional
    public InventoryResponse reserve(String sku, int quantity) {
        InventoryItem item = InventoryItem.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("SKU no encontrado: " + sku));
        if (item.availableQuantity < quantity) {
            throw new BadRequestException("Stock insuficiente para reservar " + quantity + " unidad(es)");
        }
        item.availableQuantity -= quantity;
        item.reservedQuantity += quantity;
        Panache.getEntityManager().flush();
        return InventoryResponse.from(item);
    }
}
