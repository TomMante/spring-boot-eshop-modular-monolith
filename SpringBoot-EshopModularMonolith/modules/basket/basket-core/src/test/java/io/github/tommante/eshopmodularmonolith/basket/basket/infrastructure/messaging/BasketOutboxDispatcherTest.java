package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.mapper.DTOMapper;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging.fake.FakeMessageBus;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.fake.FakeOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BasketOutboxDispatcherTest {

    private OutboxEventStore outboxEventStore;
    private FakeMessageBus messageBus;
    private BasketOutboxDispatcher dispatcher;
    private OutboxPayloadSerializer serializer;

    @BeforeEach
    void setUp() {
        messageBus = new FakeMessageBus();
        outboxEventStore = new FakeOutboxEventStore();
        var registry = new DomainEventRegistry();
        registry.registerDomainEvent("BasketCheckedOut", BasketCheckedOut.class);
        serializer = new OutboxPayloadSerializer(registry, new ObjectMapper());
        dispatcher = new BasketOutboxDispatcher(outboxEventStore, serializer, messageBus);
    }

    @Test
    @DisplayName("Given a BasketCheckedOut in the outbox, when dispatching, then it is translated to an integration event and marked processed")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var item = new Item(UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("9.99"), "socks");
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();
        var domainEvent = new BasketCheckedOut("john", List.of(item), new BigDecimal("19.98"), billingAddress, payment, shippingAddress);
        outboxEventStore.save(serializer.toStored(domainEvent));

        assertThat(outboxEventStore.fetchPending()).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchPending()).isEmpty();

        var expectedItem = new ItemDTO(item.id(), item.productId(), item.quantity(), item.priceWhenAdded(), item.productName());
        assertThat(messageBus.publishedMessages()).hasSize(1);
        assertThat(messageBus.publishedMessages())
                .containsExactly(new BasketCheckoutIntegrationEvent(domainEvent.eventId(), "john", List.of(expectedItem), new BigDecimal("19.98"), DTOMapper.toDto(billingAddress), DTOMapper.toDto(payment), DTOMapper.toDto(shippingAddress)));
    }
}
