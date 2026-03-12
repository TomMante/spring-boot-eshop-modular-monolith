package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.contract.integrationevents.ProductCreatedMessage;
import com.github.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import com.github.eshopmodularmonolith.shared.domain.DomainEvent;
import com.github.eshopmodularmonolith.shared.infrastructure.MessageBus;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcher {

    private final OutboxEventStore outboxEventStore;
    private final MessageBus messageBus;

    public OutboxDispatcher(OutboxEventStore outboxEventStore, MessageBus messageBus) {
        this.outboxEventStore = outboxEventStore;
        this.messageBus = messageBus;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        var events = outboxEventStore.fetchUnprocessedEvents();
        for (DomainEvent event : events){
            if( event instanceof ProductCreated e) {
                messageBus.publish(new ProductCreatedMessage(e.productId()));
            }
            outboxEventStore.markAsProcessed(event);
        }
    }
}
