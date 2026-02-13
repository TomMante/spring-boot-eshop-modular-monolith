package com.github.eshopmodularmonolith.shared.infrastructure;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;

import java.util.List;

public interface OutboxEventStore {

    void save(DomainEvent event);
    List<DomainEvent> fetchUnprocessedEvents();
    void markAsProcessed(DomainEvent event);
}
