package com.github.eshopmodularmonolith.basket.basket.domain.exceptions;

import java.util.UUID;

public class UnpriceableCartItem extends RuntimeException {

    public UnpriceableCartItem(UUID productId) {
        super("Product with id [" + productId + "] could not be priced for checkout.");
    }
}
