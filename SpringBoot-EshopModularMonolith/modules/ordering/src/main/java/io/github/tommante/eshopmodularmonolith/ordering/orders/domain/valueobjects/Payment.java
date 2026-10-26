package io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects;

import java.math.BigDecimal;

public record Payment(String chargeReference, BigDecimal amount, CardSummary card) {
}
