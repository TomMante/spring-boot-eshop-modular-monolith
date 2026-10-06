package io.github.tommante.eshopmodularmonolith.outboxmongo;

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
    private Instant processedOn;
    private String payload;

    public OutboxEventDocument() {
        //Required for Spring Data
    }

    OutboxEventDocument(UUID eventId, Instant occurredOn, String eventType, String payload) {
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
