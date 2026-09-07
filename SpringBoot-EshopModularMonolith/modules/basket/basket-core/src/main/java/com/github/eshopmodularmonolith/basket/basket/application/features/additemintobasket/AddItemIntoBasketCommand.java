package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import java.util.UUID;

/*
    Note: Here, instead of providing an ItemDTO, we provide the raw data so that we decouple concerns.
    As DTO is meant for transport layer, while command is for communicating intent.
    Also, not put the ShoppingCartItem directly, as that would be the result of the operation, not the input.
 */
public record AddItemIntoBasketCommand(String userName, UUID productId, int quantity) {
}
