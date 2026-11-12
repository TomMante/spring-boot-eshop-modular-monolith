package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;

public final class CatalogOutboxEvents {

    private CatalogOutboxEvents() {
    }

    public static void registerIn(DomainEventRegistry eventRegistry, OutboxSchemaRegistry schemaRegistry) {
        eventRegistry.registerDomainEvent("ProductCreated", ProductCreated.class);
        schemaRegistry.registerEvent("ProductCreated");
    }
}
