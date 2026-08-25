package com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging.fake.FakeOutboxEventStore;
import com.github.eshopmodularmonolith.shared.infrastructure.MessageBus;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BasketOutboxDispatcherTest {

    private OutboxEventStore outboxEventStore;
    private MessageBus messageBus;
    private BasketOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        messageBus = mock(MessageBus.class);
        outboxEventStore = new FakeOutboxEventStore();
        dispatcher = new BasketOutboxDispatcher(outboxEventStore, messageBus);
    }

    @Test
    @DisplayName("Given a BasketCheckedOut in the outbox, when dispatching, then it is translated to an integration event and marked processed")
    void givenOutboxMessagesToBeProcessed_whenDispatching_thenMessagesAreDispatched() {
        var item = new Item(UUID.randomUUID(), UUID.randomUUID(), 2, "red", new BigDecimal("9.99"), "socks");
        var domainEvent = new BasketCheckedOut("john", List.of(item), new BigDecimal("19.98"));
        outboxEventStore.save(domainEvent);

        assertThat(outboxEventStore.fetchUnprocessedEvents()).isNotEmpty();
        dispatcher.dispatch();
        assertThat(outboxEventStore.fetchUnprocessedEvents()).isEmpty();

        var expectedItem = new ItemDTO(item.id(), item.productId(), item.quantity(), item.color(), item.priceWhenAdded(), item.productName());
        verify(messageBus, times(1)).publish(
                new BasketCheckoutIntegrationEvent(domainEvent.eventId(), "john", List.of(expectedItem), new BigDecimal("19.98")));
    }
}
