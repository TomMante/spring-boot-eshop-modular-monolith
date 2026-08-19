package com.github.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;

import java.util.UUID;

public record RemoveItemFromBasketCommand(String userName, UUID productId) {
}
