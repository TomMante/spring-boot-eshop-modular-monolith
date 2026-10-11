package io.github.tommante.eshopmodularmonolith.outboxjpa;

import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;

import java.util.List;
import java.util.UUID;

public class JpaOutboxEventStore implements OutboxEventStore {

    private final OutboxEventJpaRepository repository;

    public JpaOutboxEventStore(OutboxEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(StoredOutboxEvent event) {
        repository.save(new OutboxEventEntity(event.eventId(), event.occurredOn(), event.eventType(), event.payload()));
    }

    @Override
    public List<StoredOutboxEvent> fetchPending() {
        return repository.findByProcessedOnNull().stream().map(OutboxEventEntity::toStored).toList();
    }

    @Override
    public void markAsProcessed(UUID eventId) {
        var entity = repository.findById(eventId).orElseThrow();
        entity.process();
        repository.save(entity);
    }
}
