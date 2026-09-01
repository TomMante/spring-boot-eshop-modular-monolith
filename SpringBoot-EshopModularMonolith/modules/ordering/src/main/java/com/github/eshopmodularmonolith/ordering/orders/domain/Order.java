package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
import com.github.eshopmodularmonolith.shared.domain.AggregateRoot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot<UUID> {

    private UUID id;
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


    private Order(UUID id, List<Item> items, String customer, Instant placedAt,
                  Address billingAddress, Payment payment,Address shippingAddress) {
        this.id = id;
        this.items = items;
        this.customer = customer;
        this.placedAt = placedAt;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.payment = payment;
    }

    public static Order create(String customer, Instant placedAt, Address billingAddress,
                               Payment payment, Address shippingAddress, List<Item> items) {
        if (billingAddress == null) throw new IllegalArgumentException("Billing address is required");
        if (payment == null) throw new IllegalArgumentException("Payment info is required");
        if (shippingAddress == null) throw new IllegalArgumentException("Shipping address is required");
        return new Order(UUID.randomUUID(), items, customer, placedAt, billingAddress, payment, shippingAddress);
    }

    public static Order reconstitute(UUID id, String customer, Instant placedAt,
                                     Address billingAddress, Payment payment, Address shippingAddress, List<Item> items) {
        return new Order(id, items, customer, placedAt, billingAddress, payment, shippingAddress);
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
}
