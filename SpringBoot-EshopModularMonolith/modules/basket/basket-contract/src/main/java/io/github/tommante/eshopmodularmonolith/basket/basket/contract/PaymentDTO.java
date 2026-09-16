package io.github.tommante.eshopmodularmonolith.basket.basket.contract;

public record PaymentDTO(
        String cardName,
        String cardNumber,
        String expiration,
        String cvv,
        int paymentMethod
) {
}
