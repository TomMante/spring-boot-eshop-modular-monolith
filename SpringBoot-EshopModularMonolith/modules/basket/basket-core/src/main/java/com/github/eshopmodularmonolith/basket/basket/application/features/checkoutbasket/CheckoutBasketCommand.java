package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;

public record CheckoutBasketCommand(String userName, Address billingAddress, Payment payment, Address shippingAddress) {
}
