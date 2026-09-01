package com.github.eshopmodularmonolith.basket.basket.domain.valueobjects;

public record Payment(
        String cardName,
        String cardNumber,
        String expiration,
        String cvv,
        int paymentMethod
) {
}
