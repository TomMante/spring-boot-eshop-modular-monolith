package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
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
                UUID.fromString("1564d3f9-b0fd-41a1-a454-1d576e3a1dc6"),
                Instant.parse("2026-10-18T05:52:45.634948Z"),
                "john",
                List.of(new Item(
                        UUID.fromString("15c964b9-8d27-4e0b-b53d-533985382476"),
                        UUID.fromString("a1000000-0000-4000-8000-000000000001"),
                        1,
                        new BigDecimal("550.00"),
                        "Galaxy Smartphone")),
                new BigDecimal("550.00"),
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
