package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import tools.jackson.databind.ObjectMapper;

public class OutboxPayloadSerializer {

    private final DomainEventRegistry registry;
    private final ObjectMapper mapper;

    public OutboxPayloadSerializer(DomainEventRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }


    public StoredOutboxEvent toStored(DomainEvent event) {
        return new StoredOutboxEvent(event.eventId(), event.occurredOn(), event.eventType(), serialize(event));
    }

    private String serialize(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize outbox event " + event.eventId() + " of type " + event.eventType(), e);
        }
    }

    public DomainEvent read(StoredOutboxEvent outboxEvent) {
        Class<? extends DomainEvent> eventType = registry.classFor(outboxEvent.eventType());
        try {
            return mapper.readValue(outboxEvent.payload(), eventType);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize " + eventType, e);
        }
    }
}
