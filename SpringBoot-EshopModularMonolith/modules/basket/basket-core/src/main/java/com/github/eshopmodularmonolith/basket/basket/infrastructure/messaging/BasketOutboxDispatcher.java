package com.github.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.integrationevents.BasketCheckoutIntegrationEvent;
import com.github.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.infrastructure.MessageBus;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BasketOutboxDispatcher {

    private final OutboxEventStore outboxEventStore;
    private final MessageBus messageBus;

    public BasketOutboxDispatcher(@Qualifier("basketOutboxEventStore") OutboxEventStore outboxEventStore, MessageBus messageBus) {
        this.outboxEventStore = outboxEventStore;
        this.messageBus = messageBus;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        var events = outboxEventStore.fetchUnprocessedEvents();
        for (DomainEvent event : events) {
            if (event instanceof BasketCheckedOut e) {
                messageBus.publish(toIntegrationEvent(e));
            }
            outboxEventStore.markAsProcessed(event);
        }
    }

    private BasketCheckoutIntegrationEvent toIntegrationEvent(BasketCheckedOut event) {
        var items = event.items().stream().map(BasketOutboxDispatcher::toDto).toList();
        return new BasketCheckoutIntegrationEvent(event.eventId(), event.userName(), items, event.totalPrice());
    }

    private static ItemDTO toDto(Item item) {
        return new ItemDTO(item.id(), item.productId(), item.quantity(), item.color(), item.priceWhenAdded(), item.productName());
    }
}
