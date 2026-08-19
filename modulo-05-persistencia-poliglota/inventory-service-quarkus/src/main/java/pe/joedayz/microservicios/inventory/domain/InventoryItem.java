package pe.joedayz.microservicios.inventory.domain;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem extends PanacheEntityBase {

    @Id
    @Column(nullable = false, length = 60)
    public String sku;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(name = "warehouse_code", nullable = false, length = 40)
    public String warehouseCode;

    @Column(name = "available_quantity", nullable = false)
    public int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    public int reservedQuantity;

    public static List<InventoryItem> listAllItems() {
        return listAll();
    }

    public static Optional<InventoryItem> findBySku(String sku) {
        return findByIdOptional(sku);
    }
}
