package com.github.eshopmodularmonolith.basket.basket.domain.port;

import com.github.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;

import java.util.Optional;

public interface BasketRepository {

    void save(ShoppingCart cart);
    Optional<ShoppingCart> findByUsername(String userName);
    void delete(String userName);
    void delete(ShoppingCart cart);
}
