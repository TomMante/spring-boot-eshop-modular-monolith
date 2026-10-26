package io.github.tommante.eshopmodularmonolith.ordering.orders.contract;

import java.math.BigDecimal;

public record PaymentDTO(String chargeReference, BigDecimal amount, CardSummaryDTO card) {
}
