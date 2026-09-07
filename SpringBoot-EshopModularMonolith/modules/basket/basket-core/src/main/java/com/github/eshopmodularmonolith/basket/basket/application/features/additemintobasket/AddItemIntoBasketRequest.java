package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddItemIntoBasketRequest(
        @NotNull(message = "{basket.basketItem.productId.notNull}") UUID productId,
        int quantity) {
}
