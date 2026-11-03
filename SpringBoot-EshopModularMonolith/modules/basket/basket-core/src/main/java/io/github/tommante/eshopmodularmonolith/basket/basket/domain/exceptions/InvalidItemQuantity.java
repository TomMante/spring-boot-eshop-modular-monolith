package io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions;

public class InvalidItemQuantity extends RuntimeException {

    public InvalidItemQuantity(int quantity) {
        super("Quantity must be at least 1, but was " + quantity + ". Send a positive quantity to add a product to the basket.");
    }
}
