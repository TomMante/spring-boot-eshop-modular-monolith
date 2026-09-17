package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
import jakarta.validation.constraints.NotBlank;

public record CheckoutPayment(
        @NotBlank(message = "{basket.checkout.payment.cardName.notBlank}") String cardName,
        @NotBlank(message = "{basket.checkout.payment.cardNumber.notBlank}") String cardNumber,
        @NotBlank(message = "{basket.checkout.payment.expiration.notBlank}") String expiration,
        @NotBlank(message = "{basket.checkout.payment.cvv.notBlank}") String cvv
) {

    Payment toDomain() {
        return new Payment(cardName, cardNumber, expiration, cvv);
    }
}
