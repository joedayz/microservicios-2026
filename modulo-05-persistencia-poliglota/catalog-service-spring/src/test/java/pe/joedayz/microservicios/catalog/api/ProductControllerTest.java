package pe.joedayz.microservicios.catalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import pe.joedayz.microservicios.catalog.api.dto.ProductRequest;
import pe.joedayz.microservicios.catalog.api.dto.ProductResponse;
import pe.joedayz.microservicios.catalog.service.ProductService;
import pe.joedayz.microservicios.catalog.tenant.TenantWebFilter;

class ProductControllerTest {

    @Test
    void shouldListProducts() {
        ProductService productService = mock(ProductService.class);
        when(productService.listProducts()).thenReturn(List.of(
                new ProductResponse("ZAP-RUN-42", "Zapatilla Running Pro", "Demo", "calzado", new BigDecimal("300.00"), "PEN")));

        ProductController controller = new ProductController(productService);

        List<ProductResponse> response = controller.list();

        assertEquals(1, response.size());
        assertEquals("ZAP-RUN-42", response.getFirst().sku());
    }

    @Test
    void shouldCreateProduct() {
        ProductService productService = mock(ProductService.class);
        ProductRequest request = new ProductRequest("NEW-01", "Nuevo producto", "Alta", "demo", new BigDecimal("15.00"), "PEN");
        when(productService.createProduct(request)).thenReturn(
                new ProductResponse("NEW-01", "Nuevo producto", "Alta", "demo", new BigDecimal("15.00"), "PEN"));

        ProductController controller = new ProductController(productService);

        ProductResponse response = controller.create(request);

        assertEquals("NEW-01", response.sku());
        assertEquals("Nuevo producto", response.name());
    }

    @Test
    void shouldRejectMissingTenantHeader() throws Exception {
        TenantWebFilter filter = new TenantWebFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertNotNull(response.getErrorMessage());
    }
}
