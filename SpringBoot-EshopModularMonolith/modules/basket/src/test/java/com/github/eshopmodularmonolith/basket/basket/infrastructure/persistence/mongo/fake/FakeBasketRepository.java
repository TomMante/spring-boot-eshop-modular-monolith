package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeBasketRepository implements BasketRepository {

    private Map<UUID, ShoppingCart> carts;

    public FakeBasketRepository() {
        carts = new HashMap<>();
    }

    @Override
    public void save(ShoppingCart cart) {
        carts.put(cart.id(), cart);
    }

    @Override
    public Optional<ShoppingCart> findById(UUID id) {
        return Optional.ofNullable(carts.get(id));
    }

    @Override
    public void delete(UUID id) {
        carts.remove(id);
    }
}
