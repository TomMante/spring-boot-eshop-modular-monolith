package io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions;

import java.math.BigDecimal;

public class ChargeDoesNotMatchTotal extends RuntimeException {

    public ChargeDoesNotMatchTotal(BigDecimal charged, BigDecimal total) {
        super("Charged amount [" + charged + "] does not match the checkout total [" + total
                + "]. Charge exactly the amount returned by totalAt for the same current prices.");
    }
}
