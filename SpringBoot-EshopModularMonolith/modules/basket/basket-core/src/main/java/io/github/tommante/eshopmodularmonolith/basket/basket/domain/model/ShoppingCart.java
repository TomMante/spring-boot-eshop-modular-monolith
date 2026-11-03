package io.github.tommante.eshopmodularmonolith.basket.basket.domain.model;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.CheckedOutItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.ChargeDoesNotMatchTotal;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.InvalidItemQuantity;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;
import io.github.tommante.eshopmodularmonolith.shared.domain.AggregateRoot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShoppingCart extends AggregateRoot<String> {

    private final String userName;
    private final List<Item> items;

    private ShoppingCart(String userName, List<Item> items) {
        this.userName = userName;
        this.items = items;
    }

    private ShoppingCart(String userName) {
        this(userName, new ArrayList<>());
    }

    @Override
    public String id() {
        return userName;
    }

    public String userName() {
        return userName;
    }

    public List<Item> items() {
        return items;
    }


    public static ShoppingCart create(String userName){
        return new ShoppingCart(userName);
    }

    public static ShoppingCart reconstitute(String userName, List<Item> items){
        return new ShoppingCart(userName, items);
    }

    public void addItem(UUID productId, int quantity, BigDecimal price, String productName){
        if (quantity < 1) {
            throw new InvalidItemQuantity(quantity);
        }
        for (int i = 0 ; i < items.size(); i++) {
            var existingItem = items.get(i);
            if (existingItem.productId().equals(productId)) {
                items.set(i, existingItem.increaseQuantity(quantity));
                return;
            }
        }
        items.add(new Item(UUID.randomUUID(), productId, quantity, price, productName));
    }

    public void removeItem(UUID productId){
        items.removeIf(item -> item.productId().equals(productId));
    }

    public BigDecimal totalAt(Map<UUID, BigDecimal> currentPrices){
        return computeTotal(checkedOutItems(currentPrices));
    }

    private List<CheckedOutItem> checkedOutItems(Map<UUID, BigDecimal> currentPrices){
        if (items.isEmpty()) {
            throw new EmptyCartCheckout();
        }
        return items.stream()
                .map(item -> priceAtCheckout(item, currentPrices))
                .toList();
    }

    private BigDecimal computeTotal(List<CheckedOutItem> checkedOutItems){
        return checkedOutItems.stream()
                .map(line -> line.price().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void checkout(UUID checkoutId, Map<UUID, BigDecimal> currentPrices, Instant checkedOutAt, Address billingAddress, Charge charge, Address shippingAddress) {

        var checkedOutItems = checkedOutItems(currentPrices);
        var totalPrice = computeTotal(checkedOutItems);

        if (charge.amount().compareTo(totalPrice) != 0) {
            throw new ChargeDoesNotMatchTotal(charge.amount(), totalPrice);
        }

        var event = new BasketCheckedOut(checkoutId, checkedOutAt, userName, checkedOutItems, totalPrice, billingAddress, charge, shippingAddress == null? billingAddress : shippingAddress);
        this.addDomainEvent(event);
    }

    private static CheckedOutItem priceAtCheckout(Item item, Map<UUID, BigDecimal> currentPrices) {
        var currentPrice = currentPrices.get(item.productId());
        if (currentPrice == null) {
            throw new UnpriceableCartItem(item.productId());
        }
        return new CheckedOutItem(item.id(), item.productId(), item.quantity(), currentPrice, item.productName());
    }
}
