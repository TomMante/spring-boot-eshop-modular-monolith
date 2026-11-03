package io.github.tommante.eshopmodularmonolith.basket.basket.domain.model;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.ChargeMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.CheckedOutItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.ChargeDoesNotMatchTotal;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.InvalidItemQuantity;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ShoppingCartTest {

    private static final Instant CHECKED_OUT_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID PRODUCT_1 = UUID.randomUUID();
    private static final UUID PRODUCT_2 = UUID.randomUUID();
    private static final Map<UUID, BigDecimal> CURRENT_PRICES =
            Map.of(PRODUCT_1, new BigDecimal("15.00"), PRODUCT_2, new BigDecimal("18.00"));

    private static ShoppingCart cartAddedAtOtherPrices() {
        var cart = ShoppingCart.create("testUser");
        cart.addItem(PRODUCT_1, 7, new BigDecimal("10.00"), "Product 1");
        cart.addItem(PRODUCT_2, 3, new BigDecimal("20.00"), "Product 2");
        return cart;
    }   // at CURRENT_PRICES: 7 × 15.00 + 3 × 18.00 = 159.00

    @Test
    @DisplayName("Given a cart whose prices changed since items were added, when the total is asked at current prices, then it is the sum of current price times quantity and no event is recorded")
    void givenCartWhosePricesChanged_whenTotalIsAskedAtCurrentPrices_thenItIsTheSumOfCurrentPriceTimesQuantityAndNoEventIsRecorded() {
        var cart = cartAddedAtOtherPrices();

        assertThat(cart.totalAt(CURRENT_PRICES)).isEqualByComparingTo("159.00");
        assertThat(cart.clearDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Given a cart item missing from the current prices, when the total is asked, then UnpriceableCartItem is thrown")
    void givenCartItemMissingFromCurrentPrices_whenTotalIsAsked_thenUnpriceableCartItemIsThrown() {
        var cart = cartAddedAtOtherPrices();

        assertThatExceptionOfType(UnpriceableCartItem.class)
                .isThrownBy(() -> cart.totalAt(Map.of(PRODUCT_1, new BigDecimal("15.00"))));
    }

    @Test
    @DisplayName("Given an empty cart, when the total is asked, then EmptyCartCheckout is thrown")
    void givenEmptyCart_whenTotalIsAsked_thenEmptyCartCheckoutIsThrown() {
        var cart = ShoppingCart.create("testUser");

        assertThatExceptionOfType(EmptyCartCheckout.class)
                .isThrownBy(() -> cart.totalAt(Map.of()));
    }

    @Test
    @DisplayName("Given a charge equal to the total but with a different scale, when checkout happens, then the cart is checked out")
    void givenChargeEqualToTotalWithDifferentScale_whenCheckoutHappens_thenCartIsCheckedOut() {
        var cart = cartAddedAtOtherPrices();
        var charge = ChargeMother.charge(new BigDecimal("159.0"));

        cart.checkout(UUID.randomUUID(), CURRENT_PRICES, CHECKED_OUT_AT,
                AddressMother.billing(), charge, AddressMother.shipping());

        assertThat(cart.clearDomainEvents()).hasSize(1);
    }

    @Test
    @DisplayName("Given a charge one cent short of the cart total, when checkout happens, then ChargeDoesNotMatchTotal is thrown and no event is recorded")
    void givenChargeOneCentShortOfTotal_whenCheckoutHappens_thenChargeDoesNotMatchTotalIsThrownAndNoEventIsRecorded() {
        var cart = cartAddedAtOtherPrices();
        var charge = ChargeMother.charge(new BigDecimal("158.99"));

        assertThatExceptionOfType(ChargeDoesNotMatchTotal.class)
                .isThrownBy(() -> cart.checkout(UUID.randomUUID(), CURRENT_PRICES, CHECKED_OUT_AT,
                        AddressMother.billing(), charge, AddressMother.shipping()));
        assertThat(cart.clearDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Given a charge matching the total, when checkout happens, then the event id is the checkout id and the event carries the charge")
    void givenChargeMatchingTotal_whenCheckoutHappens_thenEventIdIsTheCheckoutIdAndEventCarriesTheCharge() {
        var cart = cartAddedAtOtherPrices();
        var checkoutId = UUID.randomUUID();
        var charge = ChargeMother.charge(new BigDecimal("159.00"));

        cart.checkout(checkoutId, CURRENT_PRICES, CHECKED_OUT_AT,
                AddressMother.billing(), charge, AddressMother.shipping());

        var event = (BasketCheckedOut) cart.clearDomainEvents().get(0);
        assertThat(event.eventId()).isEqualTo(checkoutId);
        assertThat(event.charge()).isEqualTo(charge);
    }


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
        var currentPrices = Map.of(
                product1, new BigDecimal("15.00"),
                product2, new BigDecimal("25.00")
        ); // Assuming the total price for the items added

        var total = new BigDecimal("180.00");
        var charge = ChargeMother.charge(total);

        shoppingCart.checkout(UUID.randomUUID(), currentPrices, CHECKED_OUT_AT, billingAddress, charge, shippingAddress);

        var domainEvents = shoppingCart.clearDomainEvents();

        assertThat(domainEvents).hasSize(1);
        var event = domainEvents.get(0);
        assertThat(event).isInstanceOf(BasketCheckedOut.class);
        var basketCheckedOutEvent = (BasketCheckedOut) event;
        assertThat(basketCheckedOutEvent.totalPrice()).isEqualByComparingTo(total); // 7*15 + 3*25

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

        var total = new BigDecimal("84.00");
        var charge = ChargeMother.charge(total);

        cart.checkout(UUID.randomUUID(), currentPrices, CHECKED_OUT_AT, AddressMother.billing(), charge, AddressMother.shipping());

        var event = (BasketCheckedOut) cart.clearDomainEvents().get(0);
        assertThat(event.totalPrice()).isEqualByComparingTo(new BigDecimal("84.00")); // (2+5) * 12.00
    }

    @Test
    @DisplayName("Given a cart whose prices changed since its items were added, when checkout happens, then each checked-out line carries the checkout price")
    void givenCartWhosePricesChangedSinceItemsWereAdded_whenCheckoutHappens_thenEachCheckedOutLineCarriesTheCheckoutPrice() {
        var product1 = UUID.randomUUID();
        var product2 = UUID.randomUUID();
        var cart = ShoppingCart.create("testUser");
        cart.addItem(product1, 7, new BigDecimal("10.00"), "Product 1");
        cart.addItem(product2, 3, new BigDecimal("20.00"), "Product 2");
        var item1 = cart.items().get(0);
        var item2 = cart.items().get(1);
        var currentPrices = Map.of(product1, new BigDecimal("15.00"), product2, new BigDecimal("18.00"));

        var total = new BigDecimal("159.00");
        var charge = ChargeMother.charge(total);

        cart.checkout(UUID.randomUUID(), currentPrices, CHECKED_OUT_AT, AddressMother.billing(), charge, AddressMother.shipping());

        var event = (BasketCheckedOut) cart.clearDomainEvents().get(0);
        assertThat(event.items()).containsExactly(
                new CheckedOutItem(item1.id(), product1, 7, new BigDecimal("15.00"), "Product 1"),
                new CheckedOutItem(item2.id(), product2, 3, new BigDecimal("18.00"), "Product 2"));
        assertThat(event.totalPrice()).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("Given a shopping cart reconstituted with two items for the same product, when checkout happens, then the checked-out lines add up to the total")
    void givenCartWithDuplicateProductIdItems_whenCheckoutHappens_thenTheCheckedOutLinesAddUpToTheTotal() {
        var product = UUID.randomUUID();
        var cart = ShoppingCart.reconstitute("testUser", List.of(
                new Item(UUID.randomUUID(), product, 2, new BigDecimal("10.00"), "Product"),
                new Item(UUID.randomUUID(), product, 5, new BigDecimal("10.00"), "Product")
        ));
        var currentPrices = Map.of(product, new BigDecimal("12.00"));

        var total = new BigDecimal("84.00");
        var charge = ChargeMother.charge(total);

        cart.checkout(UUID.randomUUID(), currentPrices, CHECKED_OUT_AT, AddressMother.billing(), charge, AddressMother.shipping());

        var event = (BasketCheckedOut) cart.clearDomainEvents().get(0);
        var sumOfLines = event.items().stream()
                .map(line -> line.price().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfLines).isEqualByComparingTo(event.totalPrice());
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
    @DisplayName("Given an empty cart, when addItem is called with a quantity of zero, then InvalidItemQuantity is thrown and no line is added")
    void givenEmptyCart_whenAddItemWithQuantityZero_thenInvalidItemQuantityIsThrownAndNoLineIsAdded() {
        var cart = ShoppingCart.create("testUser");

        assertThatExceptionOfType(InvalidItemQuantity.class)
                .isThrownBy(() -> cart.addItem(PRODUCT_1, 0, new BigDecimal("10.00"), "Product 1"));

        assertThat(cart.items()).isEmpty();
    }

    @Test
    @DisplayName("Given an empty cart, when addItem is called with a negative quantity, then InvalidItemQuantity is thrown and no line is added")
    void givenEmptyCart_whenAddItemWithNegativeQuantity_thenInvalidItemQuantityIsThrownAndNoLineIsAdded() {
        var cart = ShoppingCart.create("testUser");

        assertThatExceptionOfType(InvalidItemQuantity.class)
                .isThrownBy(() -> cart.addItem(PRODUCT_1, -2, new BigDecimal("10.00"), "Product 1"));

        assertThat(cart.items()).isEmpty();
    }

    @Test
    @DisplayName("Given a cart with an existing product, when addItem is called for it with a quantity of zero, then InvalidItemQuantity is thrown and the line keeps its quantity")
    void givenCartWithExistingProduct_whenAddItemWithQuantityZero_thenInvalidItemQuantityIsThrownAndLineKeepsItsQuantity() {
        var cart = ShoppingCart.create("testUser");
        cart.addItem(PRODUCT_1, 2, new BigDecimal("10.00"), "Product 1");

        assertThatExceptionOfType(InvalidItemQuantity.class)
                .isThrownBy(() -> cart.addItem(PRODUCT_1, 0, new BigDecimal("10.00"), "Product 1"));

        assertThat(cart.items()).singleElement().extracting(Item::quantity).isEqualTo(2);
    }

    @Test
    @DisplayName("Given a quantity of zero, when an item is built, then InvalidItemQuantity is thrown")
    void givenQuantityZero_whenItemIsBuilt_thenInvalidItemQuantityIsThrown() {
        assertThatExceptionOfType(InvalidItemQuantity.class)
                .isThrownBy(() -> new Item(UUID.randomUUID(), PRODUCT_1, 0, new BigDecimal("10.00"), "Product 1"));
    }

    @Test
    @DisplayName("Given a shopping cart with an item missing from current prices, when checkout happens, then UnpriceableCartItem is thrown")
    void givenCartWithItemMissingFromCurrentPrices_whenCheckoutHappens_thenUnpriceableCartItemIsThrown() {
        var product = UUID.randomUUID();
        var shoppingCart = ShoppingCart.create("testUser");
        shoppingCart.addItem(product, 2, new BigDecimal("10.00"), "Product");

        Map<UUID, BigDecimal> currentPrices = Map.of();

        var charge = ChargeMother.charge();

        assertThatExceptionOfType(UnpriceableCartItem.class)
                .isThrownBy(() -> shoppingCart.checkout(UUID.randomUUID(), currentPrices, CHECKED_OUT_AT, AddressMother.billing(), charge, AddressMother.shipping()));

        assertThat(shoppingCart.clearDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Given an empty shopping cart, when checkout happens, then EmptyCartCheckout is thrown")
    void givenEmptyShoppingCart_whenCheckoutHappens_thenEmptyCartCheckoutIsThrown() {
        var shoppingCart = ShoppingCart.create("testUser");

        var charge = ChargeMother.charge();

        assertThatExceptionOfType(EmptyCartCheckout.class)
                .isThrownBy(() -> shoppingCart.checkout(UUID.randomUUID(), Map.of(), CHECKED_OUT_AT, AddressMother.billing(), charge,  AddressMother.shipping()));

        assertThat(shoppingCart.clearDomainEvents()).isEmpty();
    }

}