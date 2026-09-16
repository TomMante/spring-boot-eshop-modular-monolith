package io.github.tommante.eshopmodularmonolith.basket.basket.application.query;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ShoppingCartDTO;

import java.util.Optional;

public interface BasketQueryRepository {

    Optional<ShoppingCartDTO> findByUsername(String userName);
}
