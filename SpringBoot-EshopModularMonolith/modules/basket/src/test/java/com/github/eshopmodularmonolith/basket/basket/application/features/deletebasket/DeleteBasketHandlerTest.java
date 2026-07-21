package com.github.eshopmodularmonolith.basket.basket.application.features.deletebasket;

import com.github.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteBasketHandlerTest {

    private BasketRepository repository;
    private DeleteBasketHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketRepository();
        handler = new DeleteBasketHandler(repository);
    }

    @Test
    @DisplayName("Given delete basket command, when handler is called, then basket is deleted")
    void givenDeleteOrderCommand_whenHandlerIsCalled_thenOrderIsDeleted() {
        var existingCart = ShoppingCartMother.cart();
        repository.save(existingCart);
        assertThat(repository.findByUsername(existingCart.id())).isPresent().get().extracting(ShoppingCart::id).isEqualTo(existingCart.id());
        var cmd = new DeleteBasketCommand(existingCart.id());
        handler.handle(cmd);
        assertThat(repository.findByUsername(existingCart.id())).isEmpty();
    }
}