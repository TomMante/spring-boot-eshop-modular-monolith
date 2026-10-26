package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import jakarta.validation.constraints.NotBlank;

public record CheckoutPayment(
        @NotBlank(message = "{basket.checkout.payment.paymentMethodId.notBlank}") String paymentMethodId
) {
}
