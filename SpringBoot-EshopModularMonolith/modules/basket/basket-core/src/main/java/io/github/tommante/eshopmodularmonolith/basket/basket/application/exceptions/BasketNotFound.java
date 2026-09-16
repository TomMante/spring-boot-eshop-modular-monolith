package io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions;

public class BasketNotFound extends RuntimeException {
    public BasketNotFound() {
        super("Current user does not have a basket");
    }
}
