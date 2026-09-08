package com.github.eshopmodularmonolith.basket.basket.domain.exceptions;

public class EmptyCartCheckout extends RuntimeException {

    public EmptyCartCheckout() {
        super("Cannot checkout an empty cart.");
    }
}
