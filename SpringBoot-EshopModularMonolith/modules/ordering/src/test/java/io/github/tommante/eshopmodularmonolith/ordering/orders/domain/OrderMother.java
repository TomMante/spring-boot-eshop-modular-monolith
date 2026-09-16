package io.github.tommante.eshopmodularmonolith.ordering.orders.domain;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderMother {

    public static Order order() {
        return new Builder().build();
    }

    public static Builder instance() {
        return new Builder();
    }

    public static Order order(Instant placedAt) {
        return new Builder().withPlacedAt(placedAt).build();
    }

    public static Order order(UUID id, Instant placedAt) {
        return new Builder().withId(id).withPlacedAt(placedAt).build();
    }


    public static class Builder {
        private UUID id = UUID.randomUUID();
        private UUID checkoutId = UUID.randomUUID();
        private List<Item> items = new ArrayList<>(List.of(ItemMother.item()));
        private String customer = "john_doe";
        private Instant placedAt = Instant.parse("2023-01-01T00:00:00Z");
        private Address billingAddress = AddressMother.billing();
        private Payment payment = PaymentMother.payment();
        private Address shippingAddress = AddressMother.shipping();

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withPlacedAt(Instant placedAt) {
            this.placedAt = placedAt;
            return this;
        }

        public Builder withCheckoutId(UUID checkoutId){
            this.checkoutId = checkoutId;
            return this;
        }

        public Builder withCustomer(String customer) {
            this.customer = customer;
            return this;
        }

        public Order build() {
            return Order.reconstitute(id, checkoutId, customer, placedAt, billingAddress, payment, shippingAddress, items);
        }
    }
}
