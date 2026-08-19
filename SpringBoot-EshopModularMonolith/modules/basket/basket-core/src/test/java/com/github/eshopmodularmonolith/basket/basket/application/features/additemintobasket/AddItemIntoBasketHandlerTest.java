package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import com.github.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters.CatalogProductACL;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake.FakeProductQueryService;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketRepository;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AddItemIntoBasketHandlerTest {


    private BasketRepository repository;
    private FakeProductQueryService productQueryService;
    private ProductService productService;
    private AddItemIntoBasketHandler handler;

    private ProductDTO product;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketRepository();
        productQueryService = new FakeProductQueryService();
        productService = new CatalogProductACL(productQueryService);
        handler = new AddItemIntoBasketHandler(repository, productService);

        product = new ProductDTO(
                UUID.randomUUID(),
                "Hat",
                List.of("accessory"),
                "A nice hat.",
                "hat.png",
                BigDecimal.valueOf(783)
        );

        productQueryService.addProduct(product);
    }

    @Test
    @DisplayName("Given existing cart, when add item into basket command is sent, then item is added into basket")
    void givenExistingCart_whenAddItemIntoBasketCommandIsSent_thenItemIsAddedIntoBasket() {
        var existingCart = ShoppingCartMother.cart();
        repository.save(existingCart);
        assertThat(repository.findByUsername(existingCart.id())).isPresent().get().extracting(ShoppingCart::id).isEqualTo(existingCart.id());
        var cmd = new AddItemIntoBasketCommand(
                existingCart.userName(), product.id(),
                3, "Blue"
        );
        handler.handle(cmd);
        var result = repository.findByUsername(existingCart.id());
        assertThat(result).isPresent();
        var items = result.get().items();
        assertThat(items).hasSize(1);
        var item = items.getFirst();
        assertThat(item.id()).isNotNull();
        assertThat(item.productId()).isEqualTo(product.id());
        assertThat(item.quantity()).isEqualTo(cmd.quantity());
        assertThat(item.color()).isEqualTo(cmd.color());
        assertThat(item.priceWhenAdded()).isEqualTo(product.price());
        assertThat(item.productName()).isEqualTo(product.name());
    }

    @Test
    @DisplayName("Given non existing product, when trying to add it to the basket, then exception is thrown.")
    void givenNonExistingProduct_whenTryingToAddItemToBasket_thenExceptionIsThrown(){
        var existingCart = ShoppingCartMother.cart();
        repository.save(existingCart);
        assertThat(repository.findByUsername(existingCart.id())).isPresent().get().extracting(ShoppingCart::id).isEqualTo(existingCart.id());
        var cmd = new AddItemIntoBasketCommand(
                existingCart.userName(), UUID.randomUUID(),
                3, "Blue"
        );
        assertThatExceptionOfType(CatalogProductNotFound.class).isThrownBy(() -> handler.handle(cmd));
    }

    @Test
    @DisplayName("Given non existing cart, when trying to add it to the basket, then exception is thrown.")
    void givenNonExistingCart_whenTryingToAddItemToBasket_thenExceptionIsThrown(){
        var cmd = new AddItemIntoBasketCommand(
                "test", UUID.randomUUID(),
                3, "Blue"
        );
        assertThatExceptionOfType(BasketNotFound.class).isThrownBy(() -> handler.handle(cmd));
    }
}