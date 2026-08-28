package pe.joedayz.microservicios.security.inventory.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class InternalInventoryResourceTest {

    @Test
    void shouldRejectInternalCallWithoutClientId() {
        given()
                .when()
                .get("/internal/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldRejectUnknownInternalClient() {
        given()
                .header("X-Client-Id", "intruso")
                .when()
                .get("/internal/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldAllowExpectedInternalClient() {
        given()
                .header("X-Client-Id", "order-service-mtls-client")
                .when()
                .get("/internal/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE")
                .then()
                .statusCode(200)
                .body("sku", equalTo("ZAP-RUN-42"))
                .body("tenantId", equalTo("tienda-deportes"));
    }
}
