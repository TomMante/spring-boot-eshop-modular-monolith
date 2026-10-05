package io.github.tommante.eshopmodularmonolith.shared.infrastructure.fake;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;

import java.util.*;

public class FakeOutboxEventStore implements OutboxEventStore {


    private final Map<UUID, DomainEvent> storedEvents;
    private final Set<UUID> processedEvents;

    public FakeOutboxEventStore() {
        this.storedEvents = new LinkedHashMap<>();
        this.processedEvents = new HashSet<>();
    }


    @Override
    public void save(DomainEvent event) {
        storedEvents.put(event.eventId(), event);
    }

    @Override
    public List<DomainEvent> fetchUnprocessedEvents() {
        return storedEvents.values().stream().filter(e -> !processedEvents.contains(e.eventId())).toList();
    }

    @Override
    public void markAsProcessed(DomainEvent event) {
        processedEvents.add(event.eventId());
    }
}
