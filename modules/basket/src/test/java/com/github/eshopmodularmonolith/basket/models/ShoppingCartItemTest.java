package com.github.eshopmodularmonolith.basket.models;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ShoppingCartItemTest {

    @Test
    @DisplayName("Given a cart id, when creating a cart item, then id is set")
    void givenCartId_whenCreatingItem_thenIdIsSet() {
        var id = UUID.randomUUID();
        var cartItem = new ShoppingCartItem(id);

        Assertions.assertThat(cartItem).extracting(ShoppingCartItem::shoppingCartItem).isEqualTo(id);
    }


}