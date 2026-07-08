package pe.joedayz.microservicios.modulo01.patterns.cqrs;

/**
 * Lado QUERY de CQRS: expone lecturas rapidas sobre el read model, sin tocar los agregados.
 * En un microservicio real serian endpoints REST/GraphQL que consultan la BD de lecturas.
 */
public class OrderQueries {

    private final OrderReadStore readStore;

    public OrderQueries(OrderReadStore readStore) {
        this.readStore = readStore;
    }

    public OrderSummaryReadModel findSummary(String tenantId, String orderId) {
        return readStore.get(tenantId, orderId);
    }
}
