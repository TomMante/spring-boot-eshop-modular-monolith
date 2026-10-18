package io.github.tommante.eshopmodularmonolith.outboxmongo;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "outbox")
public class OutboxEventDocument {

    @Id
    private UUID eventId;
    private Instant occurredOn;
    private String eventType;
    private int schemaVersion;
    private String payload;
    private int failedAttempts;
    private Instant nextAttemptAt;
    private String lastError;
    private Instant deadLetteredOn;
    private Instant processedOn;

    public OutboxEventDocument() {
        //Required for Spring Data
    }

    OutboxEventDocument(StoredOutboxEvent event) {
        this.eventId = event.eventId();
        this.occurredOn = event.occurredOn();
        this.eventType = event.eventType();
        this.schemaVersion = event.schemaVersion();
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

    public int schemaVersion() {
        return schemaVersion;
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
        return new StoredOutboxEvent(eventId, occurredOn, eventType, schemaVersion, payload);
    }

    public DueOutboxEvent toDue() {
        return new DueOutboxEvent(this.toStored(), this.failedAttempts);
    }
}
