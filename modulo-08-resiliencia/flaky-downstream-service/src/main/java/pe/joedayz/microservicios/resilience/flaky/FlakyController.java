package pe.joedayz.microservicios.resilience.flaky;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/flaky")
public class FlakyController {

    @GetMapping("/price/{sku}")
    public ResponseEntity<Map<String, Object>> price(@PathVariable String sku) {
        return simulate(() -> Map.of(
                "sku", sku,
                "price", new BigDecimal("199.90"),
                "currency", "PEN"));
    }

    @GetMapping("/risk/{orderId}")
    public ResponseEntity<Map<String, Object>> risk(@PathVariable String orderId) {
        return simulate(() -> Map.of(
                "orderId", orderId,
                "score", ThreadLocalRandom.current().nextInt(0, 100),
                "decision", "APPROVE"));
    }

    @PostMapping("/config")
    public FlakyState.Config updateConfig(@RequestBody FlakyState.Config next) {
        return FlakyState.update(next);
    }

    @GetMapping("/config")
    public FlakyState.Config currentConfig() {
        return FlakyState.current();
    }

    // ------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> simulate(java.util.function.Supplier<Map<String, Object>> payload) {
        FlakyState.Config cfg = FlakyState.current();
        applyLatency(cfg);
        return switch (cfg.mode()) {
            case OK -> ResponseEntity.ok(payload.get());
            case FAIL -> maybeFail(cfg, payload);
            case SLOW -> {
                sleep(cfg.latencyMs() * 4L);
                yield ResponseEntity.ok(payload.get());
            }
            case TIMEOUT -> {
                sleep(30_000);
                yield ResponseEntity.ok(payload.get());
            }
        };
    }

    private ResponseEntity<Map<String, Object>> maybeFail(FlakyState.Config cfg,
            java.util.function.Supplier<Map<String, Object>> payload) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < cfg.failRate()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "SIMULATED_FAILURE", "roll", roll));
        }
        return ResponseEntity.ok(payload.get());
    }

    private void applyLatency(FlakyState.Config cfg) {
        if (cfg.latencyMs() > 0 && cfg.mode() != FlakyState.Mode.TIMEOUT) {
            sleep(cfg.latencyMs());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

