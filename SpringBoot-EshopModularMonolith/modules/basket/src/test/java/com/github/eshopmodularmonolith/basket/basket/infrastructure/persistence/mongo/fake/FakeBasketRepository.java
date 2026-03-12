package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeBasketRepository implements BasketRepository {

    private Map<String, ShoppingCart> carts;

    public FakeBasketRepository() {
        carts = new HashMap<>();
    }

    @Override
    public void save(ShoppingCart cart) {
        carts.put(cart.id(), cart);
    }

    @Override
    public Optional<ShoppingCart> findByUserName(String userName) {
        return Optional.ofNullable(carts.get(userName));
    }

    @Override
    public void delete(String userName) {
        carts.remove(userName);
    }
}
