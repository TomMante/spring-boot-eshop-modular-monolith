package io.github.tommante.eshopmodularmonolith.shared.infrastructure;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

@FunctionalInterface
public interface EventPublisher {
    void publish(DomainEvent event);
}
