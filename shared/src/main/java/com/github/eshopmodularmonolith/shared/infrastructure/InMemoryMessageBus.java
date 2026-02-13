package com.github.eshopmodularmonolith.shared.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMessageBus implements MessageBus {
    private static Logger logger = LoggerFactory.getLogger(InMemoryMessageBus.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public InMemoryMessageBus(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object message) {
        logger.info("Publishing message: {}", message);
        applicationEventPublisher.publishEvent(message);
    }
}
