package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.contract.AddressDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.ItemDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.PaymentDTO;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "{ordering.order.customerId.notNull}")
        UUID customerId,
        @NotNull(message = "{ordering.order.name.notBlank}")
        String name,
        AddressDTO shippingAddress,
        @NotNull(message = "{ordering.order.billingAddress.notNull}")
        AddressDTO billingAddress,
        @NotNull(message = "{ordering.order.payment.notNull}")
        PaymentDTO payment,
        List<ItemDTO> items
) {
}
