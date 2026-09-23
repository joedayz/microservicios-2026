package pe.joedayz.microservicios.observability.order.config;

import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * El scrape de Prometheus y los probes de Kubernetes no son el checkout.
 * Si los dejamos, Tempo se llena de ruido y la traza de la orden se esconde.
 */
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
