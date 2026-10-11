package io.github.tommante.eshopmodularmonolith.outboxcore.fake;

import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;

import java.util.*;

public class FakeOutboxEventStore implements OutboxEventStore {


    private final Map<UUID, StoredOutboxEvent> storedEvents;
    private final Set<UUID> processedEvents;

    public FakeOutboxEventStore() {
        this.storedEvents = new LinkedHashMap<>();
        this.processedEvents = new HashSet<>();
    }


    @Override
    public void save(StoredOutboxEvent event) {
        storedEvents.put(event.eventId(), event);
    }

    @Override
    public List<StoredOutboxEvent> fetchPending() {
        return storedEvents.values().stream().filter(e -> !processedEvents.contains(e.eventId())).toList();
    }

    @Override
    public void markAsProcessed(UUID eventId) {
        processedEvents.add(eventId);
    }
}
