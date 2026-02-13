package com.github.eshopmodularmonolith.catalog.products.domain.model.events;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ProductCreated(UUID productId, UUID eventId, Instant occurredOn) implements DomainEvent {

    public ProductCreated(UUID productId) {
        this(productId, UUID.randomUUID(), Instant.now());
    }

    @Override
    public String eventType() {
        return "ProductCreated";
    }
}
