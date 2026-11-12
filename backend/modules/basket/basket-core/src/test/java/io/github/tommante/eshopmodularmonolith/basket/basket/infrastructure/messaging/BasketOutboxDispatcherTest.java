package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.mapper.DTOMapper;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.CardSummaryDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.ChargeDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.CheckedOutItemDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.ChargeMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.CheckedOutItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging.fake.FakeMessageBus;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxRetryPolicy;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BasketOutboxDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private OutboxEventStore outboxEventStore;
    private FakeMessageBus messageBus;
    private BasketOutboxDispatcher dispatcher;
    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        messageBus = new FakeMessageBus();
        outboxEventStore = new FakeOutboxEventStore();
        var registry = new DomainEventRegistry();
        var schemaRegistry = new OutboxSchemaRegistry();
        BasketOutboxEvents.registerIn(registry, schemaRegistry);
        serializer = new OutboxPayloadSerializer(registry, schemaRegistry, new ObjectMapper());
        var retryPolicy = new OutboxRetryPolicy(8, Duration.ofSeconds(5), Duration.ofMinutes(5));
        dispatcher = new BasketOutboxDispatcher(outboxEventStore, serializer, retryPolicy, Clock.fixed(NOW, ZoneOffset.UTC), messageBus);
    }

    @Test
    @DisplayName("Given a BasketCheckedOut in the outbox, when dispatching, then it is translated to an integration event and marked processed")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var item = new CheckedOutItem(UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("9.99"), "socks");
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var total = new BigDecimal("19.98");
        var charge = ChargeMother.charge(total);
        var domainEvent = new BasketCheckedOut(UUID.randomUUID(), NOW, "john", List.of(item), total, billingAddress, charge,  shippingAddress);
        outboxEventStore.save(serializer.toStored(domainEvent));

        assertThat(outboxEventStore.fetchDue(Instant.MAX)).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchDue(Instant.MAX)).isEmpty();

        var expectedItem = new CheckedOutItemDTO(item.id(), item.productId(), item.quantity(), item.price(), item.productName());
        var expectedCharge = new ChargeDTO(charge.reference(), charge.amount(),
                new CardSummaryDTO(charge.card().brand(), charge.card().last4(), charge.card().expiryMonth(), charge.card().expiryYear()));
        assertThat(messageBus.publishedMessages()).hasSize(1);
        assertThat(messageBus.publishedMessages())
                .containsExactly(new BasketCheckoutIntegrationEvent(domainEvent.eventId(), "john", List.of(expectedItem), total, DTOMapper.toDto(billingAddress), expectedCharge, DTOMapper.toDto(shippingAddress)));
    }
}
