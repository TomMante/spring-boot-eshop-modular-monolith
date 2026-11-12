package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeDomainEvent;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakePricedDomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class OutboxPayloadSerializerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant OCCURRED_ON = Instant.parse("2026-01-01T00:00:00Z");

    private static final OutboxPayloadUpcaster NOTE_TO_NOTES = payload -> {
        var note = payload.remove("note");
        payload.putArray("notes").add(note);
        return payload;
    };

    private static final OutboxPayloadUpcaster NOTES_TO_INFORMATION = payload -> {
        payload.set("information", payload.remove("notes"));
        return payload;
    };

    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        var schemaRegistry = new OutboxSchemaRegistry();
        schemaRegistry.registerEvent("FakeDomainEvent");
        serializer = serializerWith(schemaRegistry);
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
        assertThat(stored.schemaVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given an event type at version 2, when stored, then the row carries version 2")
    void givenEventTypeAtVersion2_whenStored_thenRowCarriesVersion2() {
        var schemaRegistry = new OutboxSchemaRegistry();
        schemaRegistry.registerEvent("FakeDomainEvent");
        schemaRegistry.registerVersion("FakeDomainEvent", 2, payload -> payload);
        var serializerAtVersion2 = serializerWith(schemaRegistry);
        var event = new FakeDomainEvent(UUID.randomUUID(), Instant.now(), List.of("a"));

        var stored = serializerAtVersion2.toStored(event);

        assertThat(stored.schemaVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given a row of an unregistered type, when read, then it fails as an unknown event type")
    void givenRowOfUnregisteredType_whenRead_thenFailsAsUnknownEventType() {
        var stored = new StoredOutboxEvent(UUID.randomUUID(), Instant.now(), "NoSuchEvent", 1, "{}");

        assertThatThrownBy(() -> serializer.read(stored))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown event type: NoSuchEvent");
    }

    @Test
    @DisplayName("Given a row whose payload does not fit the registered class, when read, then it fails as a deserialization error")
    void givenRowWhosePayloadDoesNotFitRegisteredClass_whenRead_thenFailsAsDeserializationError() {
        var stored = new StoredOutboxEvent(UUID.randomUUID(), Instant.now(), "FakeDomainEvent",
                1, "{\"eventId\":\"not-a-uuid\",\"information\":[]}");

        assertThatThrownBy(() -> serializer.read(stored))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FakeDomainEvent")
                .hasCauseInstanceOf(MismatchedInputException.class);
    }

    @Test
    @DisplayName("Given a version 1 payload and the event at version 3, when read, then both upcasters run in order into the current record")
    void givenVersion1PayloadAndEventAtVersion3_whenRead_thenBothUpcastersRunInOrderIntoCurrentRecord() {
        var row = rowAtVersion(1, """
            {"eventId":"%s","occurredOn":"2026-01-01T00:00:00Z","note":"a"}""".formatted(EVENT_ID));

        var event = serializerAtVersion3().read(row);

        assertThat(event).isEqualTo(new FakeDomainEvent(EVENT_ID, OCCURRED_ON, List.of("a")));
    }

    @Test
    @DisplayName("Given a version 2 payload and the event at version 3, when read, then only the 2 to 3 upcaster runs")
    void givenVersion2PayloadAndEventAtVersion3_whenRead_thenOnlyThe2To3UpcasterRuns() {
        var row = rowAtVersion(2, """
            {"eventId":"%s","occurredOn":"2026-01-01T00:00:00Z","notes":["a","b"]}""".formatted(EVENT_ID));

        var event = serializerAtVersion3().read(row);

        assertThat(event).isEqualTo(new FakeDomainEvent(EVENT_ID, OCCURRED_ON, List.of("a", "b")));
    }

    @ParameterizedTest(name = "stored version {0}")
    @ValueSource(ints = {0, 4})
    @DisplayName("Given a stored version outside 1 to 3, when read, then it is rejected naming the type, the version and the readable range")
    void givenStoredVersionOutside1To3_whenRead_thenRejectedNamingTypeVersionAndReadableRange(int storedVersion) {
        var row = rowAtVersion(storedVersion, "{}");

        assertThatThrownBy(() -> serializerAtVersion3().read(row))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FakeDomainEvent")
                .hasMessageContaining("schema version " + storedVersion)
                .hasMessageContaining("versions 1 to 3");
    }

    @ParameterizedTest(name = "price {0}")
    @ValueSource(strings = {"550.00", "12345678901234567890.12"})
    @DisplayName("Given a payload with a decimal amount, when read, then the amount keeps its exact digits and scale")
    void givenPayloadWithDecimalAmount_whenRead_thenAmountKeepsItsExactDigitsAndScale(String price) {
        var registry = new DomainEventRegistry();
        registry.registerDomainEvent("FakePricedDomainEvent", FakePricedDomainEvent.class);
        var schemaRegistry = new OutboxSchemaRegistry();
        schemaRegistry.registerEvent("FakePricedDomainEvent");
        var pricedSerializer = new OutboxPayloadSerializer(registry, schemaRegistry, new ObjectMapper());
        var row = new StoredOutboxEvent(EVENT_ID, OCCURRED_ON, "FakePricedDomainEvent", 1, """
                {"eventId":"%s","occurredOn":"2026-01-01T00:00:00Z","price":%s}""".formatted(EVENT_ID, price));

        var event = pricedSerializer.read(row);

        assertThat(event).isEqualTo(new FakePricedDomainEvent(EVENT_ID, OCCURRED_ON, new BigDecimal(price)));
    }

    private static OutboxPayloadSerializer serializerAtVersion3() {
        var schemaRegistry = new OutboxSchemaRegistry();
        schemaRegistry.registerEvent("FakeDomainEvent");
        schemaRegistry.registerVersion("FakeDomainEvent", 2, NOTE_TO_NOTES);
        schemaRegistry.registerVersion("FakeDomainEvent", 3, NOTES_TO_INFORMATION);
        return serializerWith(schemaRegistry);
    }

    private static OutboxPayloadSerializer serializerWith(OutboxSchemaRegistry schemaRegistry) {
        var registry = new DomainEventRegistry();
        registry.registerDomainEvent("FakeDomainEvent", FakeDomainEvent.class);
        return new OutboxPayloadSerializer(registry, schemaRegistry, new ObjectMapper());
    }

    private static StoredOutboxEvent rowAtVersion(int schemaVersion, String payload) {
        return new StoredOutboxEvent(EVENT_ID, OCCURRED_ON, "FakeDomainEvent", schemaVersion, payload);
    }
}
