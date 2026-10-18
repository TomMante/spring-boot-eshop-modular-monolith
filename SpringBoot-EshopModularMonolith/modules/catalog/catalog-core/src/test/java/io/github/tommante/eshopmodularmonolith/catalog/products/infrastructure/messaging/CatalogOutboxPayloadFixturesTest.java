package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogOutboxPayloadFixturesTest {

    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        var eventRegistry = new DomainEventRegistry();
        var schemaRegistry = new OutboxSchemaRegistry();
        CatalogOutboxEvents.registerIn(eventRegistry, schemaRegistry);
        serializer = new OutboxPayloadSerializer(eventRegistry, schemaRegistry, new ObjectMapper());
    }

    @Test
    @DisplayName("Given the version 1 ProductCreated fixture, when read, then it is the current record with the captured values")
    void givenVersion1ProductCreatedFixture_whenRead_thenItIsTheCurrentRecordWithTheCapturedValues() throws IOException {
        var row = new StoredOutboxEvent(UUID.randomUUID(), Instant.EPOCH, "ProductCreated", 1,
                fixture("ProductCreated/v1.json"));

        var event = serializer.read(row);

        assertThat(event).isEqualTo(new ProductCreated(
                UUID.fromString("8975ff61-5e9b-45dd-9f06-a5e54dc13e19"),
                UUID.fromString("7913c1d4-c2ab-4367-9f79-82c22e2731f9"),
                Instant.parse("2026-10-18T05:41:25.238385Z")));
    }

    private String fixture(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/outbox-payloads/" + path)) {
            Objects.requireNonNull(stream, () -> "Missing fixture src/test/resources/outbox-payloads/" + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
