package io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions;

public class InvalidPaymentMethod extends RuntimeException {
    public InvalidPaymentMethod() {
        super("The payment method is unknown to the payment provider or can't be used. Obtain a new payment method id from the provider and check out again.");
    }
}
