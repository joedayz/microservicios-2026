package pe.joedayz.microservicios.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Inventario físico por SKU y tenant. Se decrementa cuando Catalog Service
 * confirma la reserva lógica de stock (evento "stock-reserved"): la reserva
 * lógica (Catalog) y el descuento físico (Inventory) están desacoplados a
 * propósito para modelar dos bounded contexts distintos.
 */
@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(nullable = false)
    private String sku;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "physical_quantity", nullable = false)
    private int physicalQuantity;

    @Version
    private long version;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, String tenantId, int physicalQuantity) {
        this.sku = sku;
        this.tenantId = tenantId;
        this.physicalQuantity = physicalQuantity;
    }

    public void decrement(int quantity) {
        this.physicalQuantity = Math.max(0, this.physicalQuantity - quantity);
    }

    public String getSku() {
        return sku;
    }

    public String getTenantId() {
        return tenantId;
    }

    public int getPhysicalQuantity() {
        return physicalQuantity;
    }
}
