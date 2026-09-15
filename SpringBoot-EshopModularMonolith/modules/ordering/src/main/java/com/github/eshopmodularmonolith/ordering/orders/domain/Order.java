package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
import com.github.eshopmodularmonolith.shared.domain.AggregateRoot;

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


    private Order(UUID id, UUID checkoutId, List<Item> items, String customer, Instant placedAt,
                  Address billingAddress, Payment payment, Address shippingAddress) {
        this.id = id;
        this.checkoutId = checkoutId;
        this.items = items;
        this.customer = customer;
        this.placedAt = placedAt;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.payment = payment;
    }

    public static Order placeFromCheckout(UUID checkoutId, String customer, Instant placedAt, Address billingAddress,
                                          Payment payment, Address shippingAddress, List<Item> items) {
        if (billingAddress == null) throw new IllegalArgumentException("Billing address is required");
        if (payment == null) throw new IllegalArgumentException("Payment info is required");
        if (shippingAddress == null) throw new IllegalArgumentException("Shipping address is required");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Order must contain at least one item");
        if (checkoutId == null) throw new IllegalArgumentException("Order requires a checkout id");
        return new Order(UUID.randomUUID(), checkoutId, items, customer, placedAt, billingAddress, payment, shippingAddress);
    }

    public static Order reconstitute(UUID id, UUID checkoutId, String customer, Instant placedAt,
                                     Address billingAddress, Payment payment, Address shippingAddress,
                                     List<Item> items) {
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
}
