package pe.joedayz.microservicios.catalog.api.dto;

import pe.joedayz.microservicios.catalog.domain.ProductStock;

public record StockResponse(String sku, String tenantId, int availableQuantity) {

    public static StockResponse from(ProductStock stock) {
        return new StockResponse(stock.getSku(), stock.getTenantId(), stock.getAvailableQuantity());
    }
}
