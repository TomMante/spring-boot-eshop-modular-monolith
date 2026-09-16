package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;
import java.util.UUID;

@TypeAlias(ItemDB.TYPE_ALIAS)
public record ItemDB(UUID id, UUID productId, int quantity, BigDecimal price) {
    public static final String TYPE_ALIAS = "Item";
}
