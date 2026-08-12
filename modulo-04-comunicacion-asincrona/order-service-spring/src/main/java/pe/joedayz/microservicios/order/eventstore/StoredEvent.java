package pe.joedayz.microservicios.order.eventstore;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Registro inmutable append-only del Event Store. Cada fila representa
 * un evento de dominio ya ocurrido para un aggregate (orderId).
 */
@Entity
@Table(name = "order_events")
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private Instant timestamp;

    protected StoredEvent() {
    }

    public StoredEvent(String aggregateId, String tenantId, String eventType,
                       String payload, long version) {
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.payload = payload;
        this.version = version;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public long getVersion() {
        return version;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
