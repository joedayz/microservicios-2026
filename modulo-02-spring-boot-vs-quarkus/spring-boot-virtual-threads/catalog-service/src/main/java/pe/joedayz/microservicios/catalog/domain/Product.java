package pe.joedayz.microservicios.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    private String name;
    private String description;
    private BigDecimal price;
    private String currency;

    protected Product() {
    }

    public String getSku() {
        return sku;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
