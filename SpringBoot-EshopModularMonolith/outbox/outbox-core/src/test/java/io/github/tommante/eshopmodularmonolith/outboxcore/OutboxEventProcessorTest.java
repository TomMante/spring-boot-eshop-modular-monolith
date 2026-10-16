package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeDomainEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OutboxEventProcessorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = NOW.plus(Duration.ofHours(1));

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
        var retryPolicy = new OutboxRetryPolicy(8, Duration.ofSeconds(5), Duration.ofMinutes(5));
        outboxEventProcessor = new OutboxEventProcessor(outboxEventStore, serializer, retryPolicy, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Given one poisoned event, when dispatching pending events, then other events are not blocked")
    void givenOnePoisonedEvent_whenDispatchingPendingEvents_thenOtherEventsAreNotBlocked() {
        var poisonedEvent = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("a"));
        var validEvent = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("b"));

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
        assertThat(outboxEventStore.fetchDue(LATER))
                .extracting(due -> due.event().eventId())
                .containsExactly(poisonedEvent.eventId());
    }

    @Test
    @DisplayName("Given an unreadable row saved before a valid one, when dispatching pending events, then the valid one is still published")
    void givenUnreadableRowBeforeValidOne_whenDispatchingPendingEvents_thenValidOneIsStillPublished() {
        var unreadableRow = new StoredOutboxEvent(UUID.randomUUID(), NOW, "FakeDomainEvent",
                "{\"eventId\":\"not-a-uuid\",\"information\":[]}");
        var validEvent = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("a"));
        outboxEventStore.save(unreadableRow);
        outboxEventStore.save(serializer.toStored(validEvent));
        List<DomainEvent> published = new ArrayList<>();

        assertThatCode(() -> outboxEventProcessor.dispatchPendingEvents(published::add))
                .doesNotThrowAnyException();

        assertThat(published).containsExactly(validEvent);
        assertThat(outboxEventStore.fetchDue(LATER))
                .extracting(due -> due.event().eventId())
                .containsExactly(unreadableRow.eventId());
    }

    @Test
    @DisplayName("Given a failing publish, when dispatching due events, then an attempt is recorded and the event is due again after the initial delay")
    void givenFailingPublish_whenDispatchingDueEvents_thenAttemptIsRecordedAndEventIsDueAgainAfterInitialDelay() {
        var event = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("a"));
        outboxEventStore.save(serializer.toStored(event));

        outboxEventProcessor.dispatchPendingEvents(failingPublisher());

        assertThat(outboxEventStore.fetchDue(NOW.plusSeconds(5).minusMillis(1))).isEmpty();
        assertThat(outboxEventStore.fetchDue(NOW.plusSeconds(5)))
                .singleElement()
                .satisfies(due -> {
                    assertThat(due.event().eventId()).isEqualTo(event.eventId());
                    assertThat(due.failedAttempts()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Given an event one attempt short of the limit, when its publish fails again, then it is dead-lettered and never fetched again")
    void givenEventOneAttemptShortOfLimit_whenPublishFailsAgain_thenItIsDeadLetteredAndNeverFetchedAgain() {
        var event = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("a"));
        outboxEventStore.save(serializer.toStored(event));
        for (int attempt = 1; attempt <= 7; attempt++) {
            outboxEventStore.recordFailedAttempt(event.eventId(), "earlier failure", NOW);
        }

        outboxEventProcessor.dispatchPendingEvents(failingPublisher());

        assertThat(outboxEventStore.fetchDue(LATER)).isEmpty();
        assertThat(outboxEventStore.isDeadLettered(event.eventId())).isTrue();
    }

    @Test
    @DisplayName("Given an event whose next attempt is in the future, when dispatching due events, then it is not published")
    void givenEventWhoseNextAttemptIsInTheFuture_whenDispatchingDueEvents_thenItIsNotPublished() {
        var event = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("a"));
        outboxEventStore.save(serializer.toStored(event));
        outboxEventStore.recordFailedAttempt(event.eventId(), "earlier failure", NOW.plusSeconds(1));
        List<DomainEvent> published = new ArrayList<>();

        outboxEventProcessor.dispatchPendingEvents(published::add);

        assertThat(published).isEmpty();
    }

    @Test
    @DisplayName("Given an event that failed before, when its publish now succeeds, then it is processed and not dead-lettered")
    void givenEventThatFailedBefore_whenPublishNowSucceeds_thenItIsProcessedAndNotDeadLettered() {
        var event = new FakeDomainEvent(UUID.randomUUID(), NOW, List.of("a"));
        outboxEventStore.save(serializer.toStored(event));
        for (int attempt = 1; attempt <= 3; attempt++) {
            outboxEventStore.recordFailedAttempt(event.eventId(), "earlier failure", NOW);
        }
        List<DomainEvent> published = new ArrayList<>();

        outboxEventProcessor.dispatchPendingEvents(published::add);

        assertThat(published).containsExactly(event);
        assertThat(outboxEventStore.fetchDue(LATER)).isEmpty();
        assertThat(outboxEventStore.isDeadLettered(event.eventId())).isFalse();
    }

    private static OutboxEventPublisher failingPublisher() {
        return event -> {
            throw new RuntimeException("Simulated publish failure");
        };
    }
}