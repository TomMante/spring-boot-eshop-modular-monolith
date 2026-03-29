package com.github.eshopmodularmonolith.basket.basket.application.features.getbasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.application.query.BasketQueryRepository;
import org.springframework.stereotype.Component;

@Component
public class GetBasketHandler {

    private BasketQueryRepository repository;

    public GetBasketHandler(BasketQueryRepository repository) {
        this.repository = repository;
    }

    public GetBasketResult handle(GetBasketQuery query){
        var basket = repository.findByUsername(query.username()).orElseThrow(BasketNotFound::new);
        return new GetBasketResult(basket);
    }

}
