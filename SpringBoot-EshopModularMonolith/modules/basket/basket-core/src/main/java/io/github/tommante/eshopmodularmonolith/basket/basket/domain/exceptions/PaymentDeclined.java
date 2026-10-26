package io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions;

public class PaymentDeclined extends RuntimeException {

    private final String code;
    private final String declineCode;

    public PaymentDeclined(String code, String declineCode) {
        super("The payment was declined (" + (declineCode == null ? code : code + "/" + declineCode) + "). Check out again with a different payment method.");
        this.code = code;
        this.declineCode = declineCode;
    }

    public String code() {
        return code;
    }

    public String declineCode() {
        return declineCode;
    }
}
