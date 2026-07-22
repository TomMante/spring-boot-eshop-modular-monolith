package com.github.eshopmodularmonolith.outboxmongo;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import tools.jackson.databind.ObjectMapper;

public class OutboxDocumentMapper {

    private final DomainEventRegistry domainEventRegistry;
    private final ObjectMapper mapper;

    public OutboxDocumentMapper(DomainEventRegistry domainEventRegistry, ObjectMapper mapper) {
        this.domainEventRegistry = domainEventRegistry;
        this.mapper = mapper;
    }

    public OutboxEventDocument fromDomainEvent(DomainEvent event) {
        return new OutboxEventDocument(event.eventId(), event.occurredOn(), event.eventType(), serialize(event));
    }

    private String serialize(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize event" + e);
        }
    }

    public DomainEvent toDomainEvent(OutboxEventDocument outboxEvent) {
        Class<? extends DomainEvent> eventType = domainEventRegistry.classFor(outboxEvent.eventType());
        try {
            return mapper.readValue(outboxEvent.payload(), eventType);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize " + eventType, e);
        }
    }
}
