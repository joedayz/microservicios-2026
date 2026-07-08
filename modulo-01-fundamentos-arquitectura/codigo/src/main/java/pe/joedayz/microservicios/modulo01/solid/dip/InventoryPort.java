package pe.joedayz.microservicios.modulo01.solid.dip;

import pe.joedayz.microservicios.modulo01.ddd.shared.Quantity;
import pe.joedayz.microservicios.modulo01.ddd.shared.Sku;
import pe.joedayz.microservicios.modulo01.tenant.TenantId;

/**
 * DIP (Dependency Inversion): PUERTO hacia el inventario.
 *
 * <p>El servicio de pedidos depende de esta abstraccion, no de un cliente HTTP concreto
 * ni de JDBC. En test se enchufa un adaptador en memoria; en produccion, uno que llama
 * por REST/gRPC al microservicio de Inventory. El dominio nunca importa detalles.
 */
public interface InventoryPort {

    /** @return true si logro reservar la cantidad para ese SKU en ese tenant. */
    boolean reserve(TenantId tenantId, Sku sku, Quantity quantity);

    /** Compensacion: libera una reserva previa (usada por la Saga al fallar un paso). */
    void release(TenantId tenantId, Sku sku, Quantity quantity);
}
