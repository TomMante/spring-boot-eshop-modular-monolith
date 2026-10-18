package io.github.tommante.eshopmodularmonolith.outboxcore.fake;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FakePricedDomainEvent(UUID eventId, Instant occurredOn, BigDecimal price) implements DomainEvent {
    @Override
    public String eventType() {
        return "FakePricedDomainEvent";
    }
}
