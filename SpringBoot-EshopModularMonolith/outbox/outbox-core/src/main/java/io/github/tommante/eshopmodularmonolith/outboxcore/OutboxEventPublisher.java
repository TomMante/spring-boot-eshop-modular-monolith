package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

@FunctionalInterface
public interface OutboxEventPublisher {
    void publish(DomainEvent event);
}
