package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.createbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
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
