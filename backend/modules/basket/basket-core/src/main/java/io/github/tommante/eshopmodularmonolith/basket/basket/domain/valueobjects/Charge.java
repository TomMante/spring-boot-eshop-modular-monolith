package io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects;

import java.math.BigDecimal;

public record Charge(String reference, BigDecimal amount, CardSummary card) {
}
