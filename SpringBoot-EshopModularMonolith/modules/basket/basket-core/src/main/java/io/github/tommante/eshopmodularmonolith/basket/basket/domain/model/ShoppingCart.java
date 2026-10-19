package io.github.tommante.eshopmodularmonolith.basket.basket.domain.model;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
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

    public void checkout(Map<UUID, BigDecimal> currentPrices, Instant checkedOutAt, Address billingAddress, Payment payment, Address shippingAddress) {
        if (items.isEmpty()) {
            throw new EmptyCartCheckout();
        }
        var totalPrice = items.stream()
                .map(i -> {
                    var currentPrice = currentPrices.get(i.productId());
                    if (currentPrice == null) {
                        throw new UnpriceableCartItem(i.productId());
                    }
                    return currentPrice.multiply(BigDecimal.valueOf(i.quantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var event = new BasketCheckedOut(checkedOutAt, userName, items, totalPrice, billingAddress, payment, shippingAddress == null? billingAddress : shippingAddress);
        this.addDomainEvent(event);
    }
}
