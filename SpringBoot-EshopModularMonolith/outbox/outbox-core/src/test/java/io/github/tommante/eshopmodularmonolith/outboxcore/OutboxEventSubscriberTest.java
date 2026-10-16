package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeDomainEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventSubscriberTest {

    @Test
    @DisplayName("Given a domain event, when the subscriber receives it, then it is saved to the outbox")
    void givenDomainEvent_whenReceived_thenSavedToOutbox() {
        var registry = new DomainEventRegistry();
        registry.registerDomainEvent("FakeDomainEvent", FakeDomainEvent.class);
        var serializer = new OutboxPayloadSerializer(registry, new ObjectMapper());
        var outbox = new FakeOutboxEventStore();
        var subscriber = new OutboxEventSubscriber(outbox, serializer);
        var event = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("a"));

        subscriber.on(event);

        assertThat(outbox.fetchDue(event.occurredOn())).singleElement()
                .extracting(due -> serializer.read(due.event()))
                .isEqualTo(event);
    }
}
