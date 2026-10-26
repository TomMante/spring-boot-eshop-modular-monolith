package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;

@TypeAlias(PaymentDB.TYPE_ALIAS)
public record PaymentDB(
        String chargeReference,
        BigDecimal amount,
        CardSummaryDB card
) {
    public static final String TYPE_ALIAS = "Payment";
}
