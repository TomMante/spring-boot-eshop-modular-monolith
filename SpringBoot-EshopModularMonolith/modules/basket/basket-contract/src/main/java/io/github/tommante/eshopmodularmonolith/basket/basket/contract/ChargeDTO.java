package io.github.tommante.eshopmodularmonolith.basket.basket.contract;

import java.math.BigDecimal;

public record ChargeDTO(String reference, BigDecimal amount, CardSummaryDTO card) {
}
