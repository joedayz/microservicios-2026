package pe.joedayz.microservicios.security.order.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.joedayz.microservicios.security.order.api.dto.InventoryCallPreviewResponse;
import pe.joedayz.microservicios.security.order.api.dto.SecurityReportResponse;
import pe.joedayz.microservicios.security.order.client.InventoryMtlsClient;
import pe.joedayz.microservicios.security.order.service.OrderService;

@RestController
@RequestMapping("/api/v1")
public class SecurityDemoController {

    private final OrderService orderService;
    private final InventoryMtlsClient inventoryMtlsClient;

    public SecurityDemoController(OrderService orderService, InventoryMtlsClient inventoryMtlsClient) {
        this.orderService = orderService;
        this.inventoryMtlsClient = inventoryMtlsClient;
    }

    @GetMapping("/security/me")
    public SecurityReportResponse currentUser(JwtAuthenticationToken authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();
        return new SecurityReportResponse(
                authentication.getName(),
                authentication.getTokenAttributes().getOrDefault("tenant_id", "n/a").toString(),
                authentication.getTokenAttributes().getOrDefault("region", "n/a").toString(),
                authorities);
    }

    @GetMapping("/admin/orders/report")
    @PreAuthorize("hasAuthority('ROLE_orders_admin')")
    public List<OrderReportLine> report() {
        return orderService.buildReport();
    }

    @GetMapping("/tenants/{tenantId}/orders/inventory-check/{sku}")
    @PreAuthorize("""
            hasAnyAuthority('SCOPE_inventory.read', 'ROLE_orders_admin')
            and @tenantClaimAuthorizer.sameTenant(authentication, #tenantId)
            and @regionClaimAuthorizer.sameRegion(authentication, #region)
            """)
    public InventoryCallPreviewResponse inventoryCheck(@PathVariable String tenantId,
                                                       @PathVariable String sku,
                                                       @RequestParam String region) {
        return inventoryMtlsClient.previewSecureCall(tenantId, sku, region);
    }

    public record OrderReportLine(String tenantId, long totalOrders) {
    }
}
