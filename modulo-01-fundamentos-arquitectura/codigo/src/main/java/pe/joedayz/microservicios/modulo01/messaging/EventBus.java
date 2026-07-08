package pe.joedayz.microservicios.modulo01.messaging;

import pe.joedayz.microservicios.modulo01.ddd.shared.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bus de eventos MUY simplificado que simula Apache Kafka (Modulo 4).
 *
 * <p>Los productores publican eventos y los consumidores suscritos los reciben. En el
 * curso real esto es un topic de Kafka con consumer groups; aqui es sincrono y en memoria
 * para que el ejemplo sea autocontenido.
 */
public class EventBus {

    private final List<Consumer<DomainEvent>> subscribers = new ArrayList<>();

    public void subscribe(Consumer<DomainEvent> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(DomainEvent event) {
        System.out.printf("   [Kafka] publicando %s (tenant=%s)%n", event.eventType(), event.tenantId());
        for (Consumer<DomainEvent> subscriber : subscribers) {
            subscriber.accept(event);
        }
    }
}
