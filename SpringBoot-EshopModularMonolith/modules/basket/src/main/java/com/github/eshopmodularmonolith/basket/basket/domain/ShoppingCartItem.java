package com.github.eshopmodularmonolith.basket.basket.domain;

import com.github.eshopmodularmonolith.shared.domain.Entity;

import java.math.BigDecimal;
import java.util.UUID;

public record ShoppingCartItem(UUID id, UUID productId, int quantity, String color, BigDecimal price, String productName) implements Entity<UUID> {
}
