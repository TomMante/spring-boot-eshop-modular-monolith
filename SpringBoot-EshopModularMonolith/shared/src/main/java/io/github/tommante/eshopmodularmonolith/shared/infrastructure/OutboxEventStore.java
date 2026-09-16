package io.github.tommante.eshopmodularmonolith.shared.infrastructure;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.util.List;

public interface OutboxEventStore {

    void save(DomainEvent event);
    List<DomainEvent> fetchUnprocessedEvents();
    void markAsProcessed(DomainEvent event);
}
