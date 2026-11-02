package io.github.tommante.eshopmodularmonolith.outboxmongo;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = OutboxEventDocument.COLLECTION_NAME)
@TypeAlias(OutboxEventDocument.TYPE_ALIAS)
public record OutboxEventDocument(
        @Id UUID eventId,
        Instant occurredOn,
        String eventType,
        int schemaVersion,
        String payload,
        int failedAttempts,
        Instant nextAttemptAt,
        String lastError,
        Instant deadLetteredOn,
        Instant processedOn) {

    public static final String COLLECTION_NAME = "outbox";
    public static final String TYPE_ALIAS = "OutboxEvent";

    static OutboxEventDocument pending(StoredOutboxEvent event) {
        return new OutboxEventDocument(
                event.eventId(),
                event.occurredOn(),
                event.eventType(),
                event.schemaVersion(),
                event.payload(),
                0,
                event.occurredOn(),
                null,
                null,
                null);
    }

    public StoredOutboxEvent toStored() {
        return new StoredOutboxEvent(eventId, occurredOn, eventType, schemaVersion, payload);
    }

    public DueOutboxEvent toDue() {
        return new DueOutboxEvent(toStored(), failedAttempts);
    }
}
