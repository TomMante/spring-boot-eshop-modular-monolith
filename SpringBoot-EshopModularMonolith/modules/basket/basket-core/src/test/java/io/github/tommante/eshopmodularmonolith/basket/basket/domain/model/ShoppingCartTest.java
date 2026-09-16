package io.github.tommante.eshopmodularmonolith.basket.basket.domain.model;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ShoppingCartTest {


    @Test
    @DisplayName("Given a shopping cart with items and a list of current prices, when checkout happens, then the cart is checked out with the correct total price")
    void givenShoppingCartWithItemsAndListOfCurrentPrices_whenCheckoutHappens_thenCartIsCheckedOutWithCorrectTotalPrice() {
        // Arrange
        var shoppingCart = ShoppingCart.create("testUser");
        var product1 = UUID.randomUUID();
        var product2 = UUID.randomUUID();
        shoppingCart.addItem(product1, 7, new BigDecimal("10.00"), "Product 1");
        shoppingCart.addItem(product2, 3, new BigDecimal("20.00"), "Product 2");

        var billingAddress = AddressMother.billing();
        var shippingAddress = AddressMother.shipping();
        var payment = PaymentMother.payment();
        var currentPrices = Map.of(
                product1, new BigDecimal("15.00"),
                product2, new BigDecimal("25.00")
        ); // Assuming the total price for the items added

        shoppingCart.checkout(currentPrices, billingAddress, payment, shippingAddress);

        var domainEvents = shoppingCart.clearDomainEvents();

        assertThat(domainEvents).hasSize(1);
        var event = domainEvents.get(0);
        assertThat(event).isInstanceOf(BasketCheckedOut.class);
        var basketCheckedOutEvent = (BasketCheckedOut) event;
        assertThat(basketCheckedOutEvent.totalPrice()).isEqualByComparingTo(new BigDecimal("180.00")); // 7*15 + 3*25

    }

    @Test
    @DisplayName("Given a shopping cart reconstituted with two items for the same product, when checkout happens, then both quantities are priced")
    void givenCartWithDuplicateProductIdItems_whenCheckoutHappens_thenBothQuantitiesArePriced() {
        var product = UUID.randomUUID();
        var cart = ShoppingCart.reconstitute("testUser", List.of(
                new Item(UUID.randomUUID(), product, 2, new BigDecimal("10.00"), "Product"),
                new Item(UUID.randomUUID(), product, 5, new BigDecimal("10.00"), "Product")
        ));
        var currentPrices = Map.of(product, new BigDecimal("12.00"));

        cart.checkout(currentPrices, AddressMother.billing(), PaymentMother.payment(), AddressMother.shipping());

        var event = (BasketCheckedOut) cart.clearDomainEvents().get(0);
        assertThat(event.totalPrice()).isEqualByComparingTo(new BigDecimal("84.00")); // (2+5) * 12.00
    }

    @Test
    @DisplayName("Given a cart with an existing product, when addItem is called with the same product, then the quantity is updated")
    void givenCartWithExistingProduct_whenAddItem_thenQuantityIsUpdated() {
        var product1 = UUID.randomUUID();
        var product2 = UUID.randomUUID();
        var cart = ShoppingCart.create("testUser");
        cart.addItem(product1, 2, new BigDecimal("10.00"), "Product");
        cart.addItem(product1, 3, new BigDecimal("10.00"), "Product");
        cart.addItem(product2, 4, new BigDecimal("20.00"), "Product 2");

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.items().get(0).quantity()).isEqualTo(5);
        assertThat(cart.items().get(1).quantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("Given a shopping cart with an item missing from current prices, when checkout happens, then UnpriceableCartItem is thrown")
    void givenCartWithItemMissingFromCurrentPrices_whenCheckoutHappens_thenUnpriceableCartItemIsThrown() {
        var product = UUID.randomUUID();
        var shoppingCart = ShoppingCart.create("testUser");
        shoppingCart.addItem(product, 2, new BigDecimal("10.00"), "Product");

        Map<UUID, BigDecimal> currentPrices = Map.of();

        assertThatExceptionOfType(UnpriceableCartItem.class)
                .isThrownBy(() -> shoppingCart.checkout(currentPrices, AddressMother.billing(), PaymentMother.payment(), AddressMother.shipping()));

        assertThat(shoppingCart.clearDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Given an empty shopping cart, when checkout happens, then EmptyCartCheckout is thrown")
    void givenEmptyShoppingCart_whenCheckoutHappens_thenEmptyCartCheckoutIsThrown() {
        var shoppingCart = ShoppingCart.create("testUser");

        assertThatExceptionOfType(EmptyCartCheckout.class)
                .isThrownBy(() -> shoppingCart.checkout(Map.of(), AddressMother.billing(), PaymentMother.payment(), AddressMother.shipping()));

        assertThat(shoppingCart.clearDomainEvents()).isEmpty();
    }

}