package io.github.tommante.eshopmodularmonolith.outboxcore.fake;

import io.github.tommante.eshopmodularmonolith.outboxcore.DueOutboxEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;

import java.time.Instant;
import java.util.*;

public class FakeOutboxEventStore implements OutboxEventStore {


    private final Map<UUID, StoredOutboxEvent> storedEvents = new LinkedHashMap<>();
    private final Map<UUID, Integer> failedAttempts = new HashMap<>();
    private final Map<UUID, Instant> nextAttemptAt = new HashMap<>();
    private final Set<UUID> processedEvents = new HashSet<>();
    private final Set<UUID> deadLetteredEvents = new HashSet<>();


    @Override
    public void save(StoredOutboxEvent event) {
        storedEvents.put(event.eventId(), event);
        failedAttempts.put(event.eventId(), 0);
        nextAttemptAt.put(event.eventId(), event.occurredOn());
    }

    @Override
    public List<DueOutboxEvent> fetchDue(Instant now) {
        return storedEvents.values().stream()
                .filter(e -> isPending(e.eventId()))
                .filter(e -> !nextAttemptAt.get(e.eventId()).isAfter(now))
                .map(e -> new DueOutboxEvent(e, failedAttempts.get(e.eventId())))
                .toList();
    }

    @Override
    public void markAsProcessed(UUID eventId, Instant processedOn) {
        processedEvents.add(eventId);
    }

    @Override
    public void recordFailedAttempt(UUID eventId, String error, Instant nextAttempt) {
        failedAttempts.merge(eventId, 1, Integer::sum);
        nextAttemptAt.put(eventId, nextAttempt);
    }

    @Override
    public void markAsDeadLettered(UUID eventId, String error, Instant deadLetteredOn) {
        failedAttempts.merge(eventId, 1, Integer::sum);
        deadLetteredEvents.add(eventId);
    }

    public boolean isDeadLettered(UUID eventId) {
        return deadLetteredEvents.contains(eventId);
    }

    private boolean isPending(UUID eventId) {
        return !processedEvents.contains(eventId) && !isDeadLettered(eventId);
    }
}
