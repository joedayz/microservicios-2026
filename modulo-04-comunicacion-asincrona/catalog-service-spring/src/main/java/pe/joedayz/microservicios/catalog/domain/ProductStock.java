package pe.joedayz.microservicios.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Read/write model del stock disponible por SKU y tenant.
 * La actualización de esta fila y el registro en el outbox ocurren en la
 * misma transacción ACID (ver {@link pe.joedayz.microservicios.catalog.saga.ReserveStockCommandHandler}).
 */
@Entity
@Table(name = "product_stock")
public class ProductStock {

    @Id
    @Column(nullable = false)
    private String sku;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    private long version;

    protected ProductStock() {
    }

    public ProductStock(String sku, String tenantId, int availableQuantity) {
        this.sku = sku;
        this.tenantId = tenantId;
        this.availableQuantity = availableQuantity;
    }

    public boolean hasEnoughStock(int quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!hasEnoughStock(quantity)) {
            throw new IllegalStateException("Stock insuficiente para sku " + sku);
        }
        this.availableQuantity -= quantity;
    }

    public String getSku() {
        return sku;
    }

    public String getTenantId() {
        return tenantId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
