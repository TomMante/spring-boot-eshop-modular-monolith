package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.PaymentDTO;
import jakarta.validation.constraints.NotNull;

public record CheckoutBasketRequest(
        @NotNull(message = "{basket.checkout.billingAddress.notNull}")
        AddressDTO billingAddress,
        @NotNull(message = "{basket.checkout.payment.notNull}")
        PaymentDTO payment,
        AddressDTO shippingAddress
) {
}
