package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;
import com.github.eshopmodularmonolith.shared.domain.AggregateRoot;

import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot<UUID> {

    private UUID id;
    private List<Item> items;
    private String customer;
    private String name;
    private Address shippingAddress;
    private Address billingAddress;
    private Payment payment;

    @Override
    public UUID id() {
        return id;
    }


    private Order(UUID id, List<Item> items, String customer, String name, Address shippingAddress,
                  Address billingAddress, Payment payment) {
        this.id = id;
        this.items = items;
        this.customer = customer;
        this.name = name;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.payment = payment;
    }

    public static Order create(String customer, String orderName, Address shippingAddress, Address billingAddress,
                               Payment payment, List<Item> items) {
        return new Order(UUID.randomUUID(), items, customer, orderName, shippingAddress, billingAddress, payment);
    }

    public static Order reconstitute(UUID id, String customer, String orderName, Address shippingAddress,
                                     Address billingAddress, Payment payment, List<Item> items) {
        return new Order(id, items, customer, orderName, shippingAddress, billingAddress, payment);
    }


    public List<Item> items() {
        return items;
    }

    public String customer() {
        return customer;
    }

    public String name() {
        return name;
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
}
