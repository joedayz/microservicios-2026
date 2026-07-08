package pe.joedayz.microservicios.modulo01.patterns.saga;

/** Señal de que un paso de la Saga fallo y deben ejecutarse las compensaciones. */
public class SagaStepFailedException extends RuntimeException {

    public SagaStepFailedException(String message) {
        super(message);
    }
}
