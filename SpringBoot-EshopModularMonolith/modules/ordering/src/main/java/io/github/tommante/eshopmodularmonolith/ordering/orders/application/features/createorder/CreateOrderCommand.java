package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.createorder;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Item;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID checkoutId,
        String customer,
        Address billingAddress,
        Payment payment,
        Address shippingAddress,
        List<Item> items
) {
}
