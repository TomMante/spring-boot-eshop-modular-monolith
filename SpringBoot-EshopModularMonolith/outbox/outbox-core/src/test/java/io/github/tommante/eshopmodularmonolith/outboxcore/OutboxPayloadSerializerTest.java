package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeDomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class OutboxPayloadSerializerTest {

    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        var registry = new DomainEventRegistry();
        registry.registerDomainEvent("FakeDomainEvent", FakeDomainEvent.class);
        serializer = new OutboxPayloadSerializer(registry, new ObjectMapper());
    }

    @Test
    @DisplayName("Given a registered domain event, when stored and read back, then it equals the original")
    void givenRegisteredDomainEvent_whenStoredAndReadBack_thenEqualsOriginal() {
        var event = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("a", "b"));

        var readBack = serializer.read(serializer.toStored(event));

        assertThat(readBack).isEqualTo(event);
    }

    @Test
    @DisplayName("Given a domain event, when stored, then the row carries its id, occurrence time and type")
    void givenDomainEvent_whenStored_thenRowCarriesItsIdOccurrenceTimeAndType() {
        var event = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("a"));

        var stored = serializer.toStored(event);

        assertThat(stored.eventId()).isEqualTo(event.eventId());
        assertThat(stored.occurredOn()).isEqualTo(event.occurredOn());
        assertThat(stored.eventType()).isEqualTo("FakeDomainEvent");
    }

    @Test
    @DisplayName("Given a row of an unregistered type, when read, then it fails as an unknown event type")
    void givenRowOfUnregisteredType_whenRead_thenFailsAsUnknownEventType() {
        var stored = new StoredOutboxEvent(UUID.randomUUID(), Instant.now(), "NoSuchEvent", "{}");

        assertThatThrownBy(() -> serializer.read(stored))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown event type: NoSuchEvent");
    }

    @Test
    @DisplayName("Given a row whose payload does not fit the registered class, when read, then it fails as a deserialization error")
    void givenRowWhosePayloadDoesNotFitRegisteredClass_whenRead_thenFailsAsDeserializationError() {
        var stored = new StoredOutboxEvent(UUID.randomUUID(), Instant.now(), "FakeDomainEvent",
                "{\"eventId\":\"not-a-uuid\",\"information\":[]}");

        assertThatThrownBy(() -> serializer.read(stored))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FakeDomainEvent")
                .hasCauseInstanceOf(MismatchedInputException.class);
    }
}