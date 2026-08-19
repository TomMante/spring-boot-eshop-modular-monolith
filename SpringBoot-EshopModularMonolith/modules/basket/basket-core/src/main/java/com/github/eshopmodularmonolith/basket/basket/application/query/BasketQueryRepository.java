package com.github.eshopmodularmonolith.basket.basket.application.query;

import com.github.eshopmodularmonolith.basket.basket.contract.ShoppingCartDTO;

import java.util.Optional;

public interface BasketQueryRepository {

    Optional<ShoppingCartDTO> findByUsername(String userName);
}
