package com.github.eshopmodularmonolith.outboxjpa;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaOutboxEventStore implements OutboxEventStore {

    private OutboxEventJpaRepository repository;
    private OutboxEventMapper eventMapper;

    public JpaOutboxEventStore(OutboxEventJpaRepository repository, OutboxEventMapper eventMapper) {
        this.repository = repository;
        this.eventMapper = eventMapper;
    }

    @Override
    public void save(DomainEvent event) {
        repository.save(eventMapper.fromDomainEvent(event));
    }

    @Override
    public List<DomainEvent> fetchUnprocessedEvents() {
        return repository.findByProcessedOnNull().stream().map(eventMapper::toDomainEvent).toList();
    }

    @Override
    public void markAsProcessed(DomainEvent event) {
        var entity = repository.findById(event.eventId()).orElseThrow();
        entity.process();
        repository.save(entity);
    }
}
