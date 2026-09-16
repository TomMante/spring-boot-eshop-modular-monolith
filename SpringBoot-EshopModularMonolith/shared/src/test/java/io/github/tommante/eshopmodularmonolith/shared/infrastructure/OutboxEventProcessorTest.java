package io.github.tommante.eshopmodularmonolith.shared.infrastructure;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.fake.FakeOutboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OutboxEventProcessorTest {


    private FakeOutboxEventStore outboxEventStore;
    private OutboxEventProcessor outboxEventProcessor;

    @BeforeEach
    void setUp() {
        outboxEventStore = new FakeOutboxEventStore();
        outboxEventProcessor = new OutboxEventProcessor(outboxEventStore);
    }

    @Test
    @DisplayName("Given one poisoned event, when dispatching pending events, then other events are not blocked")
    void givenOnePoisonedEvent_whenDispatchingPendingEvents_thenOtherEventsAreNotBlocked() {
        var poisonedEvent = new TestDomainEvent(UUID.randomUUID(), Instant.now());
        var validEvent = new TestDomainEvent(UUID.randomUUID(), Instant.now());

        outboxEventStore.save(poisonedEvent);
        outboxEventStore.save(validEvent);

        List<DomainEvent> published = new ArrayList<>();
        EventPublisher publisher = event -> {
            if (event.equals(poisonedEvent)) {
                throw new RuntimeException("Simulated publish failure");
            }
            published.add(event);
        };

        assertThatCode(() -> outboxEventProcessor.dispatchPendingEvents(publisher))
                .doesNotThrowAnyException();
        assertThat(published).containsExactly(validEvent);
        assertThat(outboxEventStore.fetchUnprocessedEvents())
                .containsExactly(poisonedEvent);
    }


    record TestDomainEvent(UUID eventId, Instant occurredOn) implements DomainEvent {
        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }
}