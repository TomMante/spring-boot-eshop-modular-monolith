package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging;


import io.github.tommante.eshopmodularmonolith.catalog.products.contract.integrationevents.ProductCreatedMessage;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging.fake.FakeMessageBus;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogOutboxDispatcherTest {


    private OutboxEventStore outboxEventStore;
    private FakeMessageBus messageBus;
    private CatalogOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        messageBus = new FakeMessageBus();
        outboxEventStore = new FakeOutboxEventStore();
        dispatcher = new CatalogOutboxDispatcher(outboxEventStore, messageBus);
    }

    @Test
    @DisplayName("Given outbox messages to be processed, when dispatching, then messages are dispatched")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var domainEvent = new ProductCreated(UUID.randomUUID());
        outboxEventStore.save(domainEvent);

        assertThat(outboxEventStore.fetchUnprocessedEvents()).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchUnprocessedEvents()).isEmpty();
        assertThat(messageBus.publishedMessages()).hasSize(1);
        assertThat(messageBus.publishedMessages()).containsExactly(new ProductCreatedMessage(domainEvent.productId()));
    }

}