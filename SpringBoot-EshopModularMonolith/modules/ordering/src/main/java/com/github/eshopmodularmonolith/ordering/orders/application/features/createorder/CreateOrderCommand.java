package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.domain.Item;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        String name,
        Address shippingAddress,
        Address billingAddress,
        Payment payment,
        List<Item> items
) {
}
