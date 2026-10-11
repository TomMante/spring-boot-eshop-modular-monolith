package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.integrationevents.ProductCreatedMessage;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventProcessor;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.MessageBus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CatalogOutboxDispatcher {

    private final OutboxEventProcessor outboxEventProcessor;
    private final MessageBus messageBus;

    public CatalogOutboxDispatcher(@Qualifier("catalogOutboxEventStore") OutboxEventStore outboxEventStore, OutboxPayloadSerializer serializer, MessageBus messageBus) {
        this.outboxEventProcessor = new OutboxEventProcessor(outboxEventStore, serializer);
        this.messageBus = messageBus;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        outboxEventProcessor.dispatchPendingEvents(this::publish);
    }

    private void publish(DomainEvent event) {
        if (event instanceof ProductCreated e) {
            messageBus.publish(new ProductCreatedMessage(e.productId()));
        }
    }
}
