package io.github.tommante.eshopmodularmonolith.basket.basket.domain;


import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;

import java.math.BigDecimal;

public class ChargeMother {

    public static Charge charge() {
        return new Builder().build();
    }

    public static Charge charge(BigDecimal amount) {
        return new Builder().withAmount(amount).build();
    }

    public static class Builder {
        private String reference = "charge-123";
        private BigDecimal amount = BigDecimal.valueOf(2312);
        private CardSummary card = new CardSummary("Visa", "4542", 8, 2028);

        public Builder withAmount(BigDecimal amount){
            this.amount = amount;
            return this;
        }


        public Charge build(){
            return new Charge(reference, amount, card);
        }
    }

}
