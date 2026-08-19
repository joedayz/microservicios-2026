package pe.joedayz.microservicios.notification.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import pe.joedayz.microservicios.notification.api.dto.NotificationRequest;
import pe.joedayz.microservicios.notification.api.dto.NotificationResponse;
import pe.joedayz.microservicios.notification.service.NotificationService;
import pe.joedayz.microservicios.notification.tenant.TenantWebFilter;

class NotificationControllerTest {

    @Test
    void shouldListNotifications() {
        NotificationService service = mock(NotificationService.class);
        when(service.listNotifications()).thenReturn(List.of(
                new NotificationResponse("n-1", "tienda-deportes", "cust-01", "EMAIL", "Pedido listo", "Hola", "PENDING", Instant.parse("2026-01-01T00:00:00Z"))));

        NotificationController controller = new NotificationController(service);

        List<NotificationResponse> response = controller.list();

        assertEquals(1, response.size());
        assertEquals("tienda-deportes", response.getFirst().tenantId());
    }

    @Test
    void shouldCreateNotification() {
        NotificationService service = mock(NotificationService.class);
        NotificationRequest request = new NotificationRequest("cust-99", "EMAIL", "Promocion", "Hola");
        when(service.createNotification(request)).thenReturn(
                new NotificationResponse("n-2", "libreria-lima", "cust-99", "EMAIL", "Promocion", "Hola", "PENDING", Instant.parse("2026-01-01T00:00:00Z")));

        NotificationController controller = new NotificationController(service);

        NotificationResponse response = controller.create(request);

        assertEquals("n-2", response.id());
        assertEquals("libreria-lima", response.tenantId());
    }

    @Test
    void shouldRejectMissingTenantHeader() throws Exception {
        TenantWebFilter filter = new TenantWebFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertNotNull(response.getErrorMessage());
    }
}
