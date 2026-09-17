package io.github.tommante.eshopmodularmonolith.ordering.orders.contract;

public record PaymentDTO(
        String cardName,
        String cardNumber,
        String expiration,
        String cvv
) {
}
