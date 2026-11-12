package io.github.tommante.eshopmodularmonolith.ordering.orders.domain;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

import java.math.BigDecimal;
import java.util.List;

public class PaymentMother {

    public static Payment payment() {
        return new Builder().build();
    }

    public static Payment payment(BigDecimal amount) {
        return new Builder().withAmount(amount).build();
    }

    public static Payment paymentFor(List<Item> items) {
        var total = items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return payment(total);
    }

    public static class Builder {
        private String chargeReference = "ch_order_test";
        private BigDecimal amount = new BigDecimal("2468.00");
        private CardSummary card = new CardSummary("visa", "4242", 8, 2030);

        public Builder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Payment build() {
            return new Payment(chargeReference, amount, card);
        }
    }
}
