package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.contract.dto.ProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RemoveItemFromBasketHandlerTest {


    private BasketRepository repository;
    private RemoveItemFromBasketHandler handler;

    private ProductDTO product;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketRepository();
        handler = new RemoveItemFromBasketHandler(repository);

        product = new ProductDTO(
                UUID.randomUUID(),
                "Hat",
                List.of("accessory"),
                "A nice hat.",
                "hat.png",
                BigDecimal.valueOf(783)
        );
    }

    @Test
    @DisplayName("Give existing cart, when remove item from basket command is sent, then item is removed from basket")
    void givenExistingCart_whenRemoveItemFromBasketCommandIsSent_thenItemIsRemovedFromBasket() {
        var existingCart = ShoppingCartMother.cart();
        existingCart.addItem(product.id(), 2, product.price(), product.name());
        repository.save(existingCart);
        assertThat(repository.findByUsername(existingCart.id())).isPresent().get().extracting(ShoppingCart::id).isEqualTo(existingCart.id());
        var cmd = new RemoveItemFromBasketCommand(
                existingCart.userName(), product.id()
        );
        handler.handle(cmd);
        var result = repository.findByUsername(existingCart.id());
        assertThat(result).isPresent();
        var items = result.get().items();
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Given non existing cart, when trying to remove product from it, then exception is thrown.")
    void givenNonExistingCart_whenTryingToRemoveProductFromIt_thenExceptionIsThrown(){
        var cmd = new RemoveItemFromBasketCommand(
                "test", UUID.randomUUID()
        );
        assertThatExceptionOfType(BasketNotFound.class).isThrownBy(() -> handler.handle(cmd));
    }
}