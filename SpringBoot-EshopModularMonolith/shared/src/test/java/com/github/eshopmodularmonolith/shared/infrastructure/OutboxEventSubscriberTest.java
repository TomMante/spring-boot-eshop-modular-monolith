package com.github.eshopmodularmonolith.shared.infrastructure;

import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

class OutboxEventSubscriberTest {

    @Test
    @DisplayName("Given a domain event, when the subscriber receives it, then it is saved to the outbox")
    void givenDomainEvent_whenReceived_thenSavedToOutbox() {
        var outbox = Mockito.mock(OutboxEventStore.class);
        var subscriber = new OutboxEventSubscriber(outbox);
        var event = new TestDomainEvent(UUID.randomUUID(), Instant.now());

        subscriber.on(event);

        verify(outbox).save(event);
    }

    record TestDomainEvent(UUID eventId, Instant occurredOn) implements DomainEvent {

        @Override
        public String eventType() {
            return "TestDomainEvent";
        }
    }
}
