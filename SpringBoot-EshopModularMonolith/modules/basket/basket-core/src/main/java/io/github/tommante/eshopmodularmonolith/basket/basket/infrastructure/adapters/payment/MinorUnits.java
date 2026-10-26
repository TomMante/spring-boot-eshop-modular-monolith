package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.payment;


import java.math.BigDecimal;

final class MinorUnits {

    private MinorUnits() {
    }

    static long of(BigDecimal amount) {
        try {
            return amount.setScale(2).movePointRight(2).longValueExact();
        } catch (ArithmeticException moreThanTwoDecimals) {
            throw new IllegalArgumentException("Amount " + amount + " has more than two decimals and can't be charged in a two-decimal currency; round it before checking out.", moreThanTwoDecimals);
        }
    }

    static BigDecimal toAmount(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, 2);
    }
}
