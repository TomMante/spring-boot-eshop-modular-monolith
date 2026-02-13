package com.github.eshopmodularmonolith.shared.domain;

import java.util.List;

public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
