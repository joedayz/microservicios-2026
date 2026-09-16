package pe.joedayz.microservicios.resilience.flaky;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Estado mutable en memoria del servicio caotico.
 * Se ajusta en runtime via POST /flaky/config para poder demostrar la apertura
 * y cierre del Circuit Breaker durante la clase.
 */
public final class FlakyState {

    public enum Mode { OK, FAIL, SLOW, TIMEOUT }

    public record Config(double failRate, long latencyMs, Mode mode) {
        public Config {
            if (failRate < 0.0 || failRate > 1.0) {
                throw new IllegalArgumentException("failRate debe estar entre 0.0 y 1.0");
            }
            if (latencyMs < 0) {
                throw new IllegalArgumentException("latencyMs no puede ser negativo");
            }
            if (mode == null) {
                mode = Mode.OK;
            }
        }
    }

    private static final AtomicReference<Config> STATE =
            new AtomicReference<>(new Config(0.0, 50, Mode.OK));

    private FlakyState() {}

    public static Config current() {
        return STATE.get();
    }

    public static Config update(Config next) {
        STATE.set(next);
        return next;
    }
}

