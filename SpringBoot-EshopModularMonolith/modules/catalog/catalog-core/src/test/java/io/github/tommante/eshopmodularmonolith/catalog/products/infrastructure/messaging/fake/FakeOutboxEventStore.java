package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging.fake;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeOutboxEventStore implements OutboxEventStore {

    private Map<UUID, DomainEvent> outboxEvents;
    private Map<UUID, Boolean> processed;

    public FakeOutboxEventStore() {
        outboxEvents = new HashMap<>();
        processed = new HashMap<>();
    }

    @Override
    public void save(DomainEvent event) {
        outboxEvents.put(event.eventId(), event);
        processed.put(event.eventId(), false);
    }

    @Override
    public List<DomainEvent> fetchUnprocessedEvents() {
        return processed.entrySet().stream()
                .filter(event -> !event.getValue())
                .map(Map.Entry::getKey)
                .map(outboxEvents::get)
                .toList();
    }

    @Override
    public void markAsProcessed(DomainEvent event) {
        processed.put(event.eventId(), true);
    }
}
