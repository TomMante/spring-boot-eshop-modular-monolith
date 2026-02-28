package com.github.eshopmodularmonolith.ordering.orders.contract;

import java.util.List;
import java.util.UUID;

public record OrderDTO(
        UUID id,
        List<ItemDTO> items,
        UUID customerId,
        String name,
        AddressDTO shippingAddress,
        AddressDTO billingAddress,
        PaymentDTO payment
) {
}
