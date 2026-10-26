package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MinorUnitsTest {

    @Test
    @DisplayName("Given an amount with two decimals, when converted, then it is the amount in cents")
    void givenAmountWithTwoDecimals_whenConverted_thenItIsTheAmountInCents() {
        assertThat(MinorUnits.of(new BigDecimal("998.00"))).isEqualTo(99800L);
    }

    @Test
    @DisplayName("Given an amount with fewer than two decimals, when converted, then it is padded to cents")
    void givenAmountWithFewerThanTwoDecimals_whenConverted_thenItIsPaddedToCents() {
        assertThat(MinorUnits.of(new BigDecimal("998"))).isEqualTo(99800L);
        assertThat(MinorUnits.of(new BigDecimal("998.5"))).isEqualTo(99850L);
    }

    @Test
    @DisplayName("Given an amount with three decimals, when converted, then it is rejected instead of rounded")
    void givenAmountWithThreeDecimals_whenConverted_thenItIsRejectedInsteadOfRounded() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MinorUnits.of(new BigDecimal("998.005")))
                .withMessageContaining("998.005");
    }

    @Test
    @DisplayName("Given a large amount, when converted, then no digits are lost")
    void givenLargeAmount_whenConverted_thenNoDigitsAreLost() {
        assertThat(MinorUnits.of(new BigDecimal("12345678.90"))).isEqualTo(1234567890L);
    }

    @Test
    @DisplayName("Given cents, when converted back, then it is the amount with two decimals")
    void givenCents_whenConvertedBack_thenItIsTheAmountWithTwoDecimals() {
        assertThat(MinorUnits.toAmount(99800L)).isEqualTo(new BigDecimal("998.00"));
    }
}