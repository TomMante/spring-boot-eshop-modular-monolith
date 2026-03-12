package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

public class ShoppingCartNotFound extends RuntimeException {
    public ShoppingCartNotFound(String userName)
    {
        super(
                "Shopping cart: {username: " + userName + " } not found."
        );
    }
}
