package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging;


import io.github.tommante.eshopmodularmonolith.catalog.products.contract.integrationevents.ProductCreatedIntegrationEvent;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging.fake.FakeMessageBus;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxRetryPolicy;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogOutboxDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");


    private OutboxEventStore outboxEventStore;
    private FakeMessageBus messageBus;
    private CatalogOutboxDispatcher dispatcher;
    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        messageBus = new FakeMessageBus();
        outboxEventStore = new FakeOutboxEventStore();
        var registry = new DomainEventRegistry();
        var schemaRegistry = new OutboxSchemaRegistry();
        CatalogOutboxEvents.registerIn(registry, schemaRegistry);
        serializer = new OutboxPayloadSerializer(registry, schemaRegistry, new ObjectMapper());
        var retryPolicy = new OutboxRetryPolicy(8, Duration.ofSeconds(5), Duration.ofMinutes(5));
        dispatcher = new CatalogOutboxDispatcher(outboxEventStore, serializer, retryPolicy, Clock.fixed(NOW, ZoneOffset.UTC), messageBus);
    }

    @Test
    @DisplayName("Given outbox messages to be processed, when dispatching, then messages are dispatched")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var domainEvent = new ProductCreated(UUID.randomUUID(), NOW);
        outboxEventStore.save(serializer.toStored(domainEvent));

        assertThat(outboxEventStore.fetchDue(Instant.MAX)).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchDue(Instant.MAX)).isEmpty();
        assertThat(messageBus.publishedMessages()).hasSize(1);
        assertThat(messageBus.publishedMessages()).containsExactly(new ProductCreatedIntegrationEvent(domainEvent.productId()));
    }

}