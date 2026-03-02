package com.github.eshopmodularmonolith.basket.basket.application.features.createbasket;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CreateBasketHandlerTest {

    private BasketRepository repository;
    private CreateBasketHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketRepository();
        handler = new CreateBasketHandler(repository);
    }


    @Test
    @DisplayName("Given create basket command, when handler is called, then an empty basket is created")
    void givenCreateBasketCommand_whenHandlerIsCalled_thenEmptyBasketIsCreated(){
        var createBasket = new CreateBasketCommand(
                "John");
        var basketID = handler.handle(createBasket);
        var basketOpt = repository.findById(basketID);
        assertThat(basketOpt).isPresent();
        var basket = basketOpt.get();
        assertThat(basket).extracting(ShoppingCart::id).isEqualTo(basketID);
        assertThat(basket).extracting(ShoppingCart::userName).isEqualTo(createBasket.userName());
        assertThat(basket).extracting(ShoppingCart::items).isEqualTo(new ArrayList<>());
    }
}