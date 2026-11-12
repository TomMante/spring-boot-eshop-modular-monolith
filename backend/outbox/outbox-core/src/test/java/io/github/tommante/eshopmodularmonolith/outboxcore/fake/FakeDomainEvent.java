package io.github.tommante.eshopmodularmonolith.outboxcore.fake;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FakeDomainEvent(UUID eventId, Instant occurredOn, List<String> information) implements DomainEvent {
    @Override
    public String eventType() {
        return "FakeDomainEvent";
    }
}
