package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

public class PaymentMother {

    public static Payment payment() {
        return new Builder().build();
    }

    public static class Builder {
        private String cardName = "Visa";
        private String cardNumber = "1232-4342-32113";
        private String expiration = "30-08";
        private String cvv = "564";
        private int paymentMethod = 1;


        public Payment build(){
            return new Payment(cardName, cardNumber, expiration, cvv, paymentMethod);
        }
    }

}
