package pe.joedayz.microservicios.catalog.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "products")
public class Product extends PanacheEntityBase {

    @Id
    @Column(name = "sku", nullable = false)
    public String sku;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    public String name;
    public String description;
    public BigDecimal price;
    public String currency;

    public static List<Product> findByTenant(String tenantId) {
        return list("tenantId", tenantId);
    }

    public static Optional<Product> findByTenantAndSku(String tenantId, String sku) {
        return find("tenantId = ?1 and sku = ?2", tenantId, sku).firstResultOptional();
    }
}
