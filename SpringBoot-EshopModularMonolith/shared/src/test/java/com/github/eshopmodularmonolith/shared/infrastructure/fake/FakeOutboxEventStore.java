package com.github.eshopmodularmonolith.shared.infrastructure.fake;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeOutboxEventStore implements OutboxEventStore {


    private final Map<UUID, DomainEvent> storedEvents;

    public FakeOutboxEventStore() {
        this.storedEvents = new LinkedHashMap<>();
    }

    @Override
    public void save(DomainEvent event) {
        storedEvents.put(event.eventId(), event);
    }

    @Override
    public List<DomainEvent> fetchUnprocessedEvents() {
        return List.copyOf(storedEvents.values());
    }

    @Override
    public void markAsProcessed(DomainEvent event) {
        storedEvents.remove(event.eventId());
    }
}
