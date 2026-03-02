package com.github.eshopmodularmonolith.basket.basket.domain.port;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;

import java.util.Optional;
import java.util.UUID;

public interface BasketRepository {

    void save(ShoppingCart cart);
    Optional<ShoppingCart> findById(UUID id);
}
