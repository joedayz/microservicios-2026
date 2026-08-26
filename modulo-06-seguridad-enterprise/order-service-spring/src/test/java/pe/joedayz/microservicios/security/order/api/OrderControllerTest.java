package pe.joedayz.microservicios.security.order.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.joedayz.microservicios.security.order.api.dto.CreateOrderRequest;
import pe.joedayz.microservicios.security.order.api.dto.OrderResponse;
import pe.joedayz.microservicios.security.order.service.OrderService;

class OrderControllerTest {

    @Test
    void shouldListOrdersForTenant() {
        OrderService orderService = mock(OrderService.class);
        when(orderService.listOrders("tienda-deportes")).thenReturn(List.of(
                new OrderResponse("ORD-1", "tienda-deportes", "ZAP-RUN-42", 2, "PE", "AUTHORIZED")));

        OrderController controller = new OrderController(orderService);

        List<OrderResponse> response = controller.list("tienda-deportes");

        assertEquals(1, response.size());
        assertEquals("ZAP-RUN-42", response.getFirst().sku());
    }

    @Test
    void shouldCreateOrder() {
        OrderService orderService = mock(OrderService.class);
        CreateOrderRequest request = new CreateOrderRequest("LIB-01", 1, "PE");
        when(orderService.createOrder("libreria-lima", request)).thenReturn(
                new OrderResponse("ORD-9", "libreria-lima", "LIB-01", 1, "PE", "AUTHORIZED"));

        OrderController controller = new OrderController(orderService);

        OrderResponse response = controller.create("libreria-lima", request);

        assertEquals("ORD-9", response.id());
        assertEquals("libreria-lima", response.tenantId());
    }
}
