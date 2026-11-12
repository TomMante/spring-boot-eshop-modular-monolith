package io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions;

public class PaymentProviderUnavailable extends RuntimeException {
    public PaymentProviderUnavailable(Throwable cause) {
        super("The payment provider could not be reached; nothing was charged. Try the checkout again later.", cause);
    }
}
