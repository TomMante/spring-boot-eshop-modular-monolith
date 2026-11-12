package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;

public final class BasketOutboxEvents {

    private BasketOutboxEvents() {
    }

    public static void registerIn(DomainEventRegistry eventRegistry, OutboxSchemaRegistry schemaRegistry) {
        eventRegistry.registerDomainEvent("BasketCheckedOut", BasketCheckedOut.class);
        schemaRegistry.registerEvent("BasketCheckedOut");
    }
}
