package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutBasketRequest(
        @NotNull(message = "{basket.checkout.billingAddress.notNull}")
        @Valid
        CheckoutAddress billingAddress,
        @NotNull(message = "{basket.checkout.payment.notNull}")
        @Valid
        CheckoutPayment payment,
        @Valid
        CheckoutAddress shippingAddress
) {
}
