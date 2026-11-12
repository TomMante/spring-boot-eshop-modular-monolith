package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;

import java.util.UUID;

public record RemoveItemFromBasketCommand(String userName, UUID productId) {
}
