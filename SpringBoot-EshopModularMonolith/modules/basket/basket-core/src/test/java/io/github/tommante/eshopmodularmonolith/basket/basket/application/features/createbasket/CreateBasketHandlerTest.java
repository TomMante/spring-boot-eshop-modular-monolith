package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.createbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

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
        var basketOpt = repository.findByUsername(basketID);
        assertThat(basketOpt).isPresent();
        var basket = basketOpt.get();
        assertThat(basket).extracting(ShoppingCart::id).isEqualTo(basketID);
        assertThat(basket).extracting(ShoppingCart::userName).isEqualTo(createBasket.userName());
        assertThat(basket).extracting(ShoppingCart::items).isEqualTo(new ArrayList<>());
    }
}