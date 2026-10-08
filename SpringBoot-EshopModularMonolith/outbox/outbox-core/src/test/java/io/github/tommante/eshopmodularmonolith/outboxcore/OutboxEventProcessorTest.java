package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeDomainEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OutboxEventProcessorTest {


    private FakeOutboxEventStore outboxEventStore;
    private OutboxEventProcessor outboxEventProcessor;
    private OutboxPayloadSerializer serializer;
    private DomainEventRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DomainEventRegistry();
        registry.registerDomainEvent("FakeDomainEvent", FakeDomainEvent.class);
        serializer = new OutboxPayloadSerializer(registry, new ObjectMapper());
        outboxEventStore = new FakeOutboxEventStore();
        outboxEventProcessor = new OutboxEventProcessor(outboxEventStore, serializer);
    }

    @Test
    @DisplayName("Given one poisoned event, when dispatching pending events, then other events are not blocked")
    void givenOnePoisonedEvent_whenDispatchingPendingEvents_thenOtherEventsAreNotBlocked() {
        var poisonedEvent = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("a"));
        var validEvent = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("b"));

        outboxEventStore.save(serializer.toStored(poisonedEvent));
        outboxEventStore.save(serializer.toStored(validEvent));

        List<DomainEvent> published = new ArrayList<>();
        OutboxEventPublisher publisher = event -> {
            if (event.equals(poisonedEvent)) {
                throw new RuntimeException("Simulated publish failure");
            }
            published.add(event);
        };

        assertThatCode(() -> outboxEventProcessor.dispatchPendingEvents(publisher))
                .doesNotThrowAnyException();
        assertThat(published).containsExactly(validEvent);
        assertThat(outboxEventStore.fetchPending()).extracting(StoredOutboxEvent::eventId)
                .containsExactly(poisonedEvent.eventId());
    }

    @Test
    @DisplayName("Given an unreadable row saved before a valid one, when dispatching pending events, then the valid one is still published")
    void givenUnreadableRowBeforeValidOne_whenDispatchingPendingEvents_thenValidOneIsStillPublished() {
        var unreadableRow = new StoredOutboxEvent(UUID.randomUUID(), Instant.now(), "FakeDomainEvent",
                "{\"eventId\":\"not-a-uuid\",\"information\":[]}");
        var validEvent = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("a"));
        outboxEventStore.save(unreadableRow);
        outboxEventStore.save(serializer.toStored(validEvent));
        List<DomainEvent> published = new ArrayList<>();

        assertThatCode(() -> outboxEventProcessor.dispatchPendingEvents(published::add))
                .doesNotThrowAnyException();

        assertThat(published).containsExactly(validEvent);
        assertThat(outboxEventStore.fetchPending())
                .extracting(StoredOutboxEvent::eventId)
                .containsExactly(unreadableRow.eventId());
    }
}