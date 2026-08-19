package pe.joedayz.microservicios.inventory.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class InventoryResourceTest {

    @Test
    void shouldListInventoryForTenant() {
        given()
                .header("X-Tenant-ID", "tienda-deportes")
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].warehouseCode", equalTo("almacen-lima"));
    }

    @Test
    void shouldRequireTenantHeader() {
        given()
                .when()
                .get("/api/v1/inventory")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldReserveStock() {
        given()
                .header("X-Tenant-ID", "libreria-lima")
                .contentType("application/json")
                .body("""
                        {
                          "quantity": 2
                        }
                        """)
                .when()
                .post("/api/v1/inventory/LIB-DDD-01/reserve")
                .then()
                .statusCode(200)
                .body("availableQuantity", equalTo(13))
                .body("reservedQuantity", equalTo(2));
    }
}
