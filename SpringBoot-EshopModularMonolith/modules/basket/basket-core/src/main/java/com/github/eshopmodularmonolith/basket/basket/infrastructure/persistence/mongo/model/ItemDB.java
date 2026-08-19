package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;
import java.util.UUID;

@TypeAlias(value = ItemDB.TYPE_ALIAS)
public record ItemDB(UUID id, UUID productId, int quantity, String color, BigDecimal price, String productName) {
    public static final String TYPE_ALIAS = "ShoppingCartItem";
}
