package com.github.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import org.springframework.stereotype.Component;

@Component
public class RemoveItemFromBasketHandler {

    private BasketRepository repository;

    public RemoveItemFromBasketHandler(BasketRepository repository) {
        this.repository = repository;
    }

    public void handle(RemoveItemFromBasketCommand cmd){
        var cart = repository.findByUsername(cmd.userName()).orElseThrow(BasketNotFound::new);
        cart.removeItem(cmd.productId());
        repository.save(cart);
    }
}
