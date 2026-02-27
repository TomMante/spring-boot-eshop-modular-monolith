package com.github.eshopmodularmonolith.ordering.orders.contract;

public record PaymentDTO(
        String cardName,
        String cardNumber,
        String expiration,
        String cvv,
        int paymentMethod
) {
}
