package io.github.tommante.eshopmodularmonolith.basket.basket.domain;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCartMother {

    public static ShoppingCart cart(){
        return new Builder().build();
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private String userName = "John";
        private List<Item> items = new ArrayList<>();


        public Builder withItems(List<Item> items){
            this.items.addAll(items);
            return this;
        }

        public ShoppingCart build() {
            return ShoppingCart.reconstitute(userName, items);
        }
    }
}
