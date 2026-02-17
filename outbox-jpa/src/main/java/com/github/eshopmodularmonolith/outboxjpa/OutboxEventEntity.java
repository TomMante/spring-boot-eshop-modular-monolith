package com.github.eshopmodularmonolith.outboxjpa;

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
    private Instant processedOn;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    public OutboxEventEntity() {
        //Required for JPA
    }

    OutboxEventEntity(UUID eventId, Instant occurredOn, String eventType, String payload) {
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.eventType = eventType;
        this.payload = payload;
    }

    public void process() {
        processedOn = Instant.now();
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

    public Instant processedOn() {
        return processedOn;
    }

    public String payload() {
        return payload;
    }
}
