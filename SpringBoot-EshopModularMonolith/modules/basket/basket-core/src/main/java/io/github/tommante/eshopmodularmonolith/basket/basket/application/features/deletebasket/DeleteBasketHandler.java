package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.deletebasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import org.springframework.stereotype.Component;

@Component
public class DeleteBasketHandler {

    private final BasketRepository repository;

    public DeleteBasketHandler(BasketRepository repository) {
        this.repository = repository;
    }

    public void handle(DeleteBasketCommand cmd){
        repository.delete(cmd.userName());
    }
}
