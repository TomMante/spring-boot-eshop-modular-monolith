package com.github.eshopmodularmonolith.basket.basket.domain;

import com.github.eshopmodularmonolith.shared.domain.AggregateRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShoppingCart extends AggregateRoot<UUID> {

    private final UUID id;
    private final String userName;
    private final List<ShoppingCartItem> items;

    private ShoppingCart(UUID id, String userName, List<ShoppingCartItem> items) {
        this.id = id;
        this.userName = userName;
        this.items = items;
    }

    private ShoppingCart(UUID id, String userName) {
        this(id, userName, new ArrayList<>());
    }

    @Override
    public UUID id() {
        return id;
    }

    public String userName() {
        return userName;
    }

    public List<ShoppingCartItem> items() {
        return items;
    }


    public static ShoppingCart create(String userName){
        return new ShoppingCart(UUID.randomUUID(), userName);
    }

    public static ShoppingCart reconstitute(UUID id, String userName, List<ShoppingCartItem> items){
        return new ShoppingCart(id, userName, items);
    }
}
