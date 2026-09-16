package io.github.tommante.eshopmodularmonolith.basket.basket.domain.model;

import io.github.tommante.eshopmodularmonolith.shared.domain.Entity;

import java.math.BigDecimal;
import java.util.UUID;

public record Item(UUID id, UUID productId, int quantity, BigDecimal priceWhenAdded, String productName) implements Entity<UUID> {

    public Item increaseQuantity(int amount) {
        return new Item(id, productId, quantity + amount, priceWhenAdded, productName);
    }
}
