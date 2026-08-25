package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.contract.AddressDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.ItemDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.PaymentDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "{ordering.order.customer.notBlank}")
        String customer,
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
