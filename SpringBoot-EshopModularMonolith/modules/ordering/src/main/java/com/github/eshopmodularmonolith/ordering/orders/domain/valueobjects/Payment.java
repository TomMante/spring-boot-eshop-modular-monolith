package com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects;

public record Payment(
        String cardName,
        String cardNumber,
        String expiration,
        String cvv,
        int paymentMethod
) {
}
