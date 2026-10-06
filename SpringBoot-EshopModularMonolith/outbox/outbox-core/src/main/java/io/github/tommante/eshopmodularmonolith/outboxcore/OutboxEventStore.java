package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.util.List;

public interface OutboxEventStore {

    void save(DomainEvent event);
    List<DomainEvent> fetchUnprocessedEvents();
    void markAsProcessed(DomainEvent event);
}
