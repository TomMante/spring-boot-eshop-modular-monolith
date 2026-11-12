package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RemoveItemFromBasketRequest(
        @NotNull(message = "{basket.basketItem.productId.notNull}") UUID productId
) {
}
