package pe.joedayz.microservicios.modulo01.patterns.cqrs;

/**
 * READ MODEL (lado Query de CQRS): vista desnormalizada y plana, optimizada para
 * la pantalla "mis pedidos". No tiene invariantes ni logica: solo datos listos para mostrar.
 *
 * <p>Se construye a partir de eventos (ver {@link OrderProjector}) y puede vivir en otra
 * base de datos (Redis, Elasticsearch, vista materializada). El WRITE MODEL sigue siendo
 * el agregado {@code Order}.
 */
public record OrderSummaryReadModel(
        String tenantId,
        String orderId,
        String total,
        String status) {
}
