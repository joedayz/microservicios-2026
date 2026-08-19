package pe.joedayz.microservicios.inventory.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class InventorySeedData {

    @Inject
    @DataSource("tienda_deportes")
    AgroalDataSource sportsDataSource;

    @Inject
    @DataSource("libreria_lima")
    AgroalDataSource booksDataSource;

    void onStart(@Observes StartupEvent event) throws Exception {
        seed(sportsDataSource, List.of(
                new Seed("ZAP-RUN-42", "Zapatilla Running Pro", "almacen-lima", 25, 0),
                new Seed("BAL-FUT-01", "Balon Match Pro", "almacen-lima", 40, 0)));
        seed(booksDataSource, List.of(
                new Seed("LIB-DDD-01", "Domain-Driven Design", "almacen-centro", 15, 0),
                new Seed("JAVA-25-01", "Java 25 Developer Guide", "almacen-centro", 20, 0)));
    }

    private void seed(AgroalDataSource dataSource, List<Seed> items) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement countStatement = connection.prepareStatement("SELECT COUNT(*) FROM inventory_items");
             ResultSet resultSet = countStatement.executeQuery()) {
            resultSet.next();
            if (resultSet.getInt(1) > 0) {
                return;
            }
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO inventory_items (sku, name, warehouse_code, available_quantity, reserved_quantity)
                     VALUES (?, ?, ?, ?, ?)
                     """)) {
            for (Seed item : items) {
                insert.setString(1, item.sku());
                insert.setString(2, item.name());
                insert.setString(3, item.warehouseCode());
                insert.setInt(4, item.availableQuantity());
                insert.setInt(5, item.reservedQuantity());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private record Seed(String sku, String name, String warehouseCode, int availableQuantity, int reservedQuantity) {
    }
}
