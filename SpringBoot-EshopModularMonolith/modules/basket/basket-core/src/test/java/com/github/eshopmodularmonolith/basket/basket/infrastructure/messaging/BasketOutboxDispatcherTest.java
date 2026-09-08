package com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.application.mapper.DTOMapper;
import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.basket.basket.domain.AddressMother;
import com.github.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import com.github.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging.fake.FakeMessageBus;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging.fake.FakeOutboxEventStore;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BasketOutboxDispatcherTest {

    private OutboxEventStore outboxEventStore;
    private FakeMessageBus messageBus;
    private BasketOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        messageBus = new FakeMessageBus();
        outboxEventStore = new FakeOutboxEventStore();
        dispatcher = new BasketOutboxDispatcher(outboxEventStore, messageBus);
    }

    @Test
    @DisplayName("Given a BasketCheckedOut in the outbox, when dispatching, then it is translated to an integration event and marked processed")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var item = new Item(UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("9.99"), "socks");
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();
        var domainEvent = new BasketCheckedOut("john", List.of(item), new BigDecimal("19.98"), billingAddress, payment, shippingAddress);
        outboxEventStore.save(domainEvent);

        assertThat(outboxEventStore.fetchUnprocessedEvents()).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchUnprocessedEvents()).isEmpty();

        var expectedItem = new ItemDTO(item.id(), item.productId(), item.quantity(), item.priceWhenAdded(), item.productName());
        assertThat(messageBus.publishedMessages()).hasSize(1);
        assertThat(messageBus.publishedMessages())
                .containsExactly(new BasketCheckoutIntegrationEvent(domainEvent.eventId(), "john", List.of(expectedItem), new BigDecimal("19.98"), DTOMapper.toDto(billingAddress), DTOMapper.toDto(payment), DTOMapper.toDto(shippingAddress)));
    }
}
