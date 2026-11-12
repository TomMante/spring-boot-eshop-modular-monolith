package io.github.tommante.eshopmodularmonolith.outboxjpa;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JpaOutboxEventStore implements OutboxEventStore {

    private final OutboxEventJpaRepository repository;

    public JpaOutboxEventStore(OutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(StoredOutboxEvent event) {
        repository.save(new OutboxEventEntity(event));
    }

    @Override
    public List<DueOutboxEvent> fetchDue(Instant now) {
        return repository.findDue(now).stream()
                .map(OutboxEventEntity::toDue)
                .toList();
    }

    @Override
    public void markAsProcessed(UUID eventId, Instant processedOn) {
        requireMatched(eventId, repository.markAsProcessed(eventId, processedOn));
    }

    @Override
    public void recordFailedAttempt(UUID eventId, String error, Instant nextAttemptAt) {
        requireMatched(eventId, repository.recordFailedAttempt(eventId, error, nextAttemptAt));
    }

    @Override
    public void markAsDeadLettered(UUID eventId, String error, Instant deadLetteredOn) {
        requireMatched(eventId, repository.markAsDeadLettered(eventId, error, deadLetteredOn));
    }

    private static void requireMatched(UUID eventId, int matchedRows) {
        if (matchedRows == 0) {
            throw new IllegalStateException("Unknown outbox event: " + eventId);
        }
    }
}
