package pe.joedayz.microservicios.modulo01.patterns.saga;

/** Resultado de ejecutar la Saga: confirmada o cancelada (con motivo). */
public record SagaResult(boolean success, String detail) {

    public static SagaResult confirmed() {
        return new SagaResult(true, "Pedido confirmado");
    }

    public static SagaResult cancelled(String reason) {
        return new SagaResult(false, "Pedido cancelado: " + reason);
    }
}
