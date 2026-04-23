package com.github.eshopmodularmonolith.basket.basket.domain;

import com.github.eshopmodularmonolith.shared.domain.AggregateRoot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShoppingCart extends AggregateRoot<String> {

    private final String userName;
    private final List<Item> items;

    private ShoppingCart(String userName, List<Item> items) {
        this.userName = userName;
        this.items = items;
    }

    private ShoppingCart(String userName) {
        this(userName, new ArrayList<>());
    }

    @Override
    public String id() {
        return userName;
    }

    public String userName() {
        return userName;
    }

    public List<Item> items() {
        return items;
    }


    public static ShoppingCart create(String userName){
        return new ShoppingCart(userName);
    }

    public static ShoppingCart reconstitute(String userName, List<Item> items){
        return new ShoppingCart(userName, items);
    }

    public void addItem(UUID productId, int quantity, String color, BigDecimal price, String productName){
        var item = new Item(UUID.randomUUID(), productId, quantity, color, price, productName);
        items.add(item);
    }

    public void removeItem(UUID productId){
        items.removeIf(item -> item.productId().equals(productId));
    }
}
