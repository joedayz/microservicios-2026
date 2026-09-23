package pe.joedayz.microservicios.observability.flaky.config;

import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

@Component
class ActuatorObservationPredicate implements ObservationPredicate {

    @Override
    public boolean test(String name, Context context) {
        if (context instanceof ServerRequestObservationContext server) {
            String uri = server.getCarrier().getRequestURI();
            return uri == null || !uri.startsWith("/actuator");
        }
        return true;
    }
}
