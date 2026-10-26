package io.github.tommante.eshopmodularmonolith.ordering.orders.domain;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
import io.github.tommante.eshopmodularmonolith.shared.domain.AggregateRoot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot<UUID> {

    private UUID id;
    private UUID checkoutId;
    private List<Item> items;
    private String customer;
    private Instant placedAt;
    private Address shippingAddress;
    private Address billingAddress;
    private Payment payment;

    @Override
    public UUID id() {
        return id;
    }


    private Order(UUID id, UUID checkoutId, List<Item> items, String customer, Instant placedAt, Address billingAddress, Payment payment, Address shippingAddress) {
        this.id = id;
        this.checkoutId = checkoutId;
        this.items = items;
        this.customer = customer;
        this.placedAt = placedAt;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.payment = payment;
    }

    public static Order placeFromCheckout(UUID checkoutId, String customer, Instant placedAt, Address billingAddress, Payment payment, Address shippingAddress, List<Item> items) {
        if (billingAddress == null) throw new IllegalArgumentException("Billing address is required");
        if (payment == null) throw new IllegalArgumentException("Payment info is required");
        if (shippingAddress == null) throw new IllegalArgumentException("Shipping address is required");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Order must contain at least one item");
        for (var item : items) {
            if (item.price() == null)
                throw new IllegalArgumentException("Order item " + item.id() + " has no price; every item must carry the price paid at checkout");
        }
        if (checkoutId == null) throw new IllegalArgumentException("Order requires a checkout id");
        var order = new Order(UUID.randomUUID(), checkoutId, items, customer, placedAt, billingAddress, payment, shippingAddress);
        if (payment.amount() == null || payment.amount().compareTo(order.totalPrice()) != 0)
            throw new IllegalArgumentException("Payment amount " + payment.amount() + " does not match the order total " + order.totalPrice() + "; an order can only be placed for exactly the amount that was charged");
        return order;
    }

    public static Order reconstitute(UUID id, UUID checkoutId, String customer, Instant placedAt, Address billingAddress, Payment payment, Address shippingAddress, List<Item> items) {
        return new Order(id, checkoutId, items, customer, placedAt, billingAddress, payment, shippingAddress);
    }


    public List<Item> items() {
        return items;
    }

    public String customer() {
        return customer;
    }

    public Address shippingAddress() {
        return shippingAddress;
    }

    public Address billingAddress() {
        return billingAddress;
    }

    public Payment payment() {
        return payment;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public UUID checkoutId() {
        return checkoutId;
    }

    public BigDecimal totalPrice() {
        return items.stream().map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
