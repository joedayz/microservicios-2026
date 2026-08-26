package pe.joedayz.microservicios.security.order.api;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.security.order.api.dto.CreateOrderRequest;
import pe.joedayz.microservicios.security.order.api.dto.OrderResponse;
import pe.joedayz.microservicios.security.order.service.OrderService;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("""
            hasAnyAuthority('ROLE_orders_reader', 'ROLE_orders_writer', 'ROLE_orders_admin')
            and @tenantClaimAuthorizer.sameTenant(authentication, #tenantId)
            """)
    public List<OrderResponse> list(@PathVariable String tenantId) {
        return orderService.listOrders(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("""
            hasAnyAuthority('ROLE_orders_writer', 'ROLE_orders_admin')
            and @tenantClaimAuthorizer.sameTenant(authentication, #tenantId)
            and @regionClaimAuthorizer.sameRegion(authentication, #request.shippingRegion())
            """)
    public OrderResponse create(@PathVariable String tenantId, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(tenantId, request);
    }
}
