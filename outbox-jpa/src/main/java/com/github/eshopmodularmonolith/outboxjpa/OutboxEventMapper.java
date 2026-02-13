package com.github.eshopmodularmonolith.outboxjpa;


import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Component
public class OutboxEventMapper {

    private final DomainEventRegistry domainEventRegistry;
    private final ObjectMapper mapper;

    public OutboxEventMapper(DomainEventRegistry domainEventRegistry, ObjectMapper mapper) {
        this.domainEventRegistry = domainEventRegistry;
        this.mapper = mapper;
    }

    public OutboxEventEntity fromDomainEvent(DomainEvent event) {
        return new OutboxEventEntity(event.eventId(), event.occurredOn(), event.eventType(), serialize(event));
    }

    private String serialize(DomainEvent event){
        try {
            return mapper.writeValueAsString(event);
        }catch (Exception e) {
            throw new IllegalStateException("Cannot serialize event" + e);
        }
    }

    public DomainEvent toDomainEvent(OutboxEventEntity outboxEvent){
        Class<? extends DomainEvent> eventType = domainEventRegistry.classFor(outboxEvent.eventType());
        try {
            return mapper.readValue(outboxEvent.payload(), eventType);
        }catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize " + eventType, e);
        }
    }
}
