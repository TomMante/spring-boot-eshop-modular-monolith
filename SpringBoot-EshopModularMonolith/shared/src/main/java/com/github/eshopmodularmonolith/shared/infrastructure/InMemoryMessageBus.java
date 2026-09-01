package com.github.eshopmodularmonolith.shared.infrastructure;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMessageBus implements MessageBus {

    private final ApplicationEventPublisher applicationEventPublisher;

    public InMemoryMessageBus(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object message) {
        applicationEventPublisher.publishEvent(message);
    }
}
