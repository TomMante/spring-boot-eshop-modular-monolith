package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.CheckedOutItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.outboxcore.StoredOutboxEvent;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BasketOutboxPayloadFixturesTest {

    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        var eventRegistry = new DomainEventRegistry();
        var schemaRegistry = new OutboxSchemaRegistry();
        BasketOutboxEvents.registerIn(eventRegistry, schemaRegistry);
        serializer = new OutboxPayloadSerializer(eventRegistry, schemaRegistry, new ObjectMapper());
    }

    @Test
    @DisplayName("Given the version 1 BasketCheckedOut fixture, when read, then it is the current record with the captured values")
    void givenVersion1BasketCheckedOutFixture_whenRead_thenItIsTheCurrentRecordWithTheCapturedValues() throws IOException {
        var row = new StoredOutboxEvent(UUID.randomUUID(), Instant.EPOCH, "BasketCheckedOut", 1,
                fixture("BasketCheckedOut/v1.json"));

        var event = serializer.read(row);

        assertThat(event).isEqualTo(new BasketCheckedOut(
                UUID.fromString("5d10104a-4d4b-4356-a566-e4806494fda9"),
                Instant.parse("2026-10-19T08:49:01.710Z"),
                "john",
                List.of(new CheckedOutItem(
                        UUID.fromString("3ba7a9ff-ceed-41c5-ada8-13bf83d59b28"),
                        UUID.fromString("a1000000-0000-4000-8000-000000000001"),
                        2,
                        new BigDecimal("499.00"),
                        "Galaxy Smartphone")),
                new BigDecimal("998.00"),
                new Address("John", "Doe", "john.doe@example.com", "1 Billing Street", "USA", "Billing State", "10001"),
                new Payment("John Doe", "4111111111111111", "12/29", "123"),
                new Address("Jane", "Roe", "jane.roe@example.com", "2 Shipping Avenue", "USA", "Shipping State", "90210")));
    }

    private String fixture(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/outbox-payloads/" + path)) {
            Objects.requireNonNull(stream, () -> "Missing fixture src/test/resources/outbox-payloads/" + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
