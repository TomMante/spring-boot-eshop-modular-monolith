package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.query.BasketQueryRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.ShoppingCartDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeBasketQueryRepository implements BasketQueryRepository {

    private Map<String, ShoppingCartDTO> carts;

    public FakeBasketQueryRepository() {
        carts = new HashMap<>();
    }


    public void add(ShoppingCartDTO cart) {
        carts.put(cart.userName(), cart);
    }

    @Override
    public Optional<ShoppingCartDTO> findByUsername(String userName) {
        return Optional.ofNullable(carts.get(userName));
    }
}
