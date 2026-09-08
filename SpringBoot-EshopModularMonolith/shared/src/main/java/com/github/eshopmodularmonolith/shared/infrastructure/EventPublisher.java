package com.github.eshopmodularmonolith.shared.infrastructure;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;

@FunctionalInterface
public interface EventPublisher {
    void publish(DomainEvent event);
}
