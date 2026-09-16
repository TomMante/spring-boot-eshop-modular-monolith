package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging.fake;

import io.github.tommante.eshopmodularmonolith.shared.infrastructure.MessageBus;

import java.util.List;

public class FakeMessageBus implements MessageBus {

    private final List<Object> publishedMessages;

    public FakeMessageBus() {
        this.publishedMessages = new java.util.ArrayList<>();
    }

    @Override
    public void publish(Object message) {
        publishedMessages.add(message);
    }

    public List<Object> publishedMessages() {
        return publishedMessages;
    }
}
