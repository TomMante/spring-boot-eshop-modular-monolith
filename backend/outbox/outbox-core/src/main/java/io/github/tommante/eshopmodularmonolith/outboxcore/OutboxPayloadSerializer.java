package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class OutboxPayloadSerializer {

    private final DomainEventRegistry eventRegistry;
    private final OutboxSchemaRegistry schemaRegistry;
    private final ObjectMapper mapper;

    public OutboxPayloadSerializer(DomainEventRegistry eventRegistry, OutboxSchemaRegistry schemaRegistry, ObjectMapper mapper) {
        this.eventRegistry = eventRegistry;
        this.schemaRegistry = schemaRegistry;
        this.mapper = mapper;
    }


    public StoredOutboxEvent toStored(DomainEvent event) {
        return new StoredOutboxEvent(
                event.eventId(),
                event.occurredOn(),
                event.eventType(),
                schemaRegistry.currentVersionOf(event.eventType()),
                serialize(event));
    }

    private String serialize(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize outbox event " + event.eventId() + " of type " + event.eventType(), e);
        }
    }

    public DomainEvent read(StoredOutboxEvent outboxEvent) {
        Class<? extends DomainEvent> eventClass = eventRegistry.classFor(outboxEvent.eventType());
        var upcasters = schemaRegistry.upcastersFrom(outboxEvent.eventType(), outboxEvent.schemaVersion());
        try {
            // Without this, the tree holds decimals as double and loses the payload's digits and scale
            var payload = (ObjectNode) mapper.reader()
                    .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .readTree(outboxEvent.payload());
            for (var upcaster : upcasters)
                payload = upcaster.upcast(payload);
            return mapper.treeToValue(payload, eventClass);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read " + outboxEvent.eventType() + " payload at schema version "
                    + outboxEvent.schemaVersion() + " into " + eventClass.getName(), e);
        }
    }

}
