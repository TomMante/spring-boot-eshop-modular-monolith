package io.github.tommante.eshopmodularmonolith.outboxjpa;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID eventId;
    @Column(nullable = false)
    private Instant occurredOn;
    @Column(nullable = false)
    private String eventType;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;
    @Column(nullable = false)
    private int failedAttempts;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    @Column(columnDefinition = "TEXT")
    private String lastError;
    private Instant deadLetteredOn;
    private Instant processedOn;

    public OutboxEventEntity() {
        //Required for JPA
    }

    OutboxEventEntity(StoredOutboxEvent event) {
        this.eventId = event.eventId();
        this.occurredOn = event.occurredOn();
        this.eventType = event.eventType();
        this.payload = event.payload();
        this.nextAttemptAt = event.occurredOn();
    }

    public UUID eventId() {
        return eventId;
    }

    public Instant occurredOn() {
        return occurredOn;
    }

    public String eventType() {
        return eventType;
    }

    public String payload() {
        return payload;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public String lastError() {
        return lastError;
    }

    public Instant deadLetteredOn() {
        return deadLetteredOn;
    }

    public Instant processedOn() {
        return processedOn;
    }

    public StoredOutboxEvent toStored() {
        return new StoredOutboxEvent(eventId, occurredOn, eventType, payload);
    }

    public DueOutboxEvent toDue() {
        return new DueOutboxEvent(toStored(), failedAttempts);
    }

}
