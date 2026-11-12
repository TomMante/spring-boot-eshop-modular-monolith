package io.github.tommante.eshopmodularmonolith.outboxcore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventStore {

    void save(StoredOutboxEvent event);
    List<DueOutboxEvent> fetchDue(Instant now);
    void markAsProcessed(UUID eventId, Instant processedOn);
    void recordFailedAttempt(UUID eventId, String error, Instant nextAttemptAt);
    void markAsDeadLettered(UUID eventId, String error, Instant deadLetteredOn);
}
