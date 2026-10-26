package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;

public record CheckoutBasketCommand(String userName, Address billingAddress, String paymentMethodId, Address shippingAddress) {
}
