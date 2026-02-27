package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderMother {

    public static Order order() {
        return new Builder().build();
    }


    public static class Builder {
        private UUID id = UUID.randomUUID();
        private List<Item> items = new ArrayList<>(List.of(ItemMother.item()));
        private UUID customerId = UUID.randomUUID();
        private String orderName = "First Order";
        private Address shippingAddress = AddressMother.address();
        private Address billingAddress =  AddressMother.address();
        private Payment payment = PaymentMother.payment();

        public Order build() {
            return Order.reconstitute(id, customerId, orderName, shippingAddress, billingAddress, payment, items);
        }
    }
}
