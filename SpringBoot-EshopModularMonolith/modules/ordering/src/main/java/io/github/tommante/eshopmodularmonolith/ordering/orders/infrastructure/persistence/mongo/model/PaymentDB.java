package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias(PaymentDB.TYPE_ALIAS)
public record PaymentDB(
        String cardName,
        String cardNumber,
        String expiration,
        String cvv,
        int paymentMethod
) {

    public static final String TYPE_ALIAS = "Payment";
}
