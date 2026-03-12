package com.github.eshopmodularmonolith.basket.basket.application.features.createbasket;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import org.springframework.stereotype.Component;

@Component
public class CreateBasketHandler {

    private BasketRepository repository;

    public CreateBasketHandler(BasketRepository repository) {
        this.repository = repository;
    }

    public String handle(CreateBasketCommand cmd) {
        var basket = ShoppingCart.create(cmd.userName());
        repository.save(basket);
        return basket.id();
    }
}
