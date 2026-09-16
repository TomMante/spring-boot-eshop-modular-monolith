package io.github.tommante.eshopmodularmonolith.ordering.orders.contract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDTO(
        UUID id,
        List<ItemDTO> items,
        String customer,
        Instant placedAt,
        AddressDTO shippingAddress,
        AddressDTO billingAddress,
        PaymentDTO payment
) {
}
