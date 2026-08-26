package pe.joedayz.microservicios.security.inventory.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;

@QuarkusTest
class InventoryResourceTest {

    @Test
    void shouldRejectAnonymousRequests() {
        given()
                .when()
                .get("/api/v1/tenants/tienda-deportes/inventory")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "ana-reader", roles = {"inventory_viewer"})
    @OidcSecurity(claims = {
            @Claim(key = "tenant_id", value = "tienda-deportes"),
            @Claim(key = "region", value = "PE")
    })
    void shouldListInventoryForOwnTenant() {
        given()
                .when()
                .get("/api/v1/tenants/tienda-deportes/inventory")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].sku", equalTo("POL-TRK-09"));
    }

    @Test
    @TestSecurity(user = "ana-reader", roles = {"inventory_viewer"})
    @OidcSecurity(claims = {
            @Claim(key = "tenant_id", value = "tienda-deportes"),
            @Claim(key = "region", value = "PE")
    })
    void shouldRejectOtherTenant() {
        given()
                .when()
                .get("/api/v1/tenants/libreria-lima/inventory")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "ana-reader", roles = {"inventory_viewer"})
    @OidcSecurity(claims = {
            @Claim(key = "tenant_id", value = "tienda-deportes"),
            @Claim(key = "region", value = "PE")
    })
    void shouldRejectReserveForViewerRole() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "quantity": 1,
                          "region": "PE"
                        }
                        """)
                .when()
                .post("/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42/reserve")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "bruno-manager", roles = {"inventory_manager"})
    @OidcSecurity(claims = {
            @Claim(key = "tenant_id", value = "tienda-deportes"),
            @Claim(key = "region", value = "PE")
    })
    void shouldReserveStockForManagerInSameTenantAndRegion() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "quantity": 2,
                          "region": "PE"
                        }
                        """)
                .when()
                .post("/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42/reserve")
                .then()
                .statusCode(200)
                .body("availableQuantity", equalTo(23))
                .body("reservedQuantity", equalTo(5));
    }

    @Test
    @TestSecurity(user = "bruno-manager", roles = {"inventory_manager"})
    @OidcSecurity(claims = {
            @Claim(key = "tenant_id", value = "tienda-deportes"),
            @Claim(key = "region", value = "PE")
    })
    void shouldRejectReserveInOtherRegion() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "quantity": 1,
                          "region": "US"
                        }
                        """)
                .when()
                .post("/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42/reserve")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "carla-admin", roles = {"inventory_admin"})
    @OidcSecurity(claims = {
            @Claim(key = "tenant_id", value = "plataforma"),
            @Claim(key = "region", value = "LATAM")
    })
    void shouldAllowAdminToCrossTenants() {
        given()
                .when()
                .get("/api/v1/tenants/libreria-lima/inventory")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].sku", equalTo("LIB-DDD-01"));
    }
}
