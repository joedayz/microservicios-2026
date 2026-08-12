package pe.joedayz.microservicios.order.api.dto;

import java.time.Instant;

import pe.joedayz.microservicios.order.domain.Order;

public record OrderResponse(
        String orderId,
        String tenantId,
        String customerId,
        String sku,
        int quantity,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getTenantId(),
                order.getCustomerId(),
                order.getSku(),
                order.getQuantity(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
