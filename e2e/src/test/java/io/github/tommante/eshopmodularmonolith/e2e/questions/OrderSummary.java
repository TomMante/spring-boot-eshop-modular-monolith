package io.github.tommante.eshopmodularmonolith.e2e.questions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record OrderSummary(List<OrderLine> lines, BigDecimal total, String cardBrand, String cardLast4) {

    public OrderSummary {
        // The API answers 1100.0, the feature says 1100.00: compare money at cents.
        total = total.setScale(2, RoundingMode.UNNECESSARY);
    }
}
