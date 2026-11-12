package io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProductCreated(UUID productId, UUID eventId, Instant occurredOn) implements DomainEvent {

    public ProductCreated(UUID productId, Instant occurredOn) {
        this(productId, UUID.randomUUID(), occurredOn);
    }

    @Override
    public String eventType() {
        return "ProductCreated";
    }
}
