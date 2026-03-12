package com.github.eshopmodularmonolith.basket.basket.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShoppingCartMother {

    public static ShoppingCart cart(){
        return new Builder().build();
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String userName = "John";
        private List<ShoppingCartItem> items = new ArrayList<>();


        public ShoppingCart build() {
            return ShoppingCart.reconstitute(userName, items);
        }
    }
}
