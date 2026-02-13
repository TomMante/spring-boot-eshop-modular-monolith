package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;


import com.github.eshopmodularmonolith.catalog.products.contract.integrationevents.ProductCreatedMessage;
import com.github.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeOutboxEventStore;
import com.github.eshopmodularmonolith.shared.infrastructure.MessageBus;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OutboxDispatcherTest {


    private OutboxEventStore outboxEventStore;
    private MessageBus messageBus;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        messageBus = Mockito.mock(MessageBus.class);
        outboxEventStore = new FakeOutboxEventStore();
        dispatcher = new OutboxDispatcher(outboxEventStore, messageBus);
    }

    @Test
    @DisplayName("Given outbox messages to be processed, when dispatching, then messages are dispatched")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var domainEvent = new ProductCreated(UUID.randomUUID());
        outboxEventStore.save(domainEvent);

        assertThat(outboxEventStore.fetchUnprocessedEvents()).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchUnprocessedEvents()).isEmpty();
        verify(messageBus, times(1)).publish(new ProductCreatedMessage(domainEvent.productId()));
    }

}