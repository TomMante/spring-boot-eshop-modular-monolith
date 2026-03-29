package com.github.eshopmodularmonolith.basket.basket.domain.port;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;

import java.util.Optional;

public interface BasketRepository {

    void save(ShoppingCart cart);
    Optional<ShoppingCart> findByUsername(String userName);
    void delete(String userName);
}
