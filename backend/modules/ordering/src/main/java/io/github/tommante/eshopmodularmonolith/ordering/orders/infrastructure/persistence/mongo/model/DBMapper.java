package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.dto.*;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Item;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Order;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

public class DBMapper {


    public static AddressDB toDb(Address address){
        return new AddressDB(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static Address toDomain(AddressDB address){
        return new Address(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static PaymentDB toDb(Payment payment) {
        var card = payment.card();
        return new PaymentDB(payment.chargeReference(), payment.amount(),
                new CardSummaryDB(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }

    public static Payment toDomain(PaymentDB payment) {
        var card = payment.card();
        return new Payment(payment.chargeReference(), payment.amount(),
                new CardSummary(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }

    public static PaymentDTO toDto(PaymentDB payment) {
        var card = payment.card();
        return new PaymentDTO(payment.chargeReference(), payment.amount(),
                new CardSummaryDTO(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }

    public static ItemDB toDb(Item item){
        return new ItemDB(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static Item toDomain(ItemDB item){
        return new Item(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static OrderDB toDb(Order order){
        return new OrderDB(order.id(), order.checkoutId(), order.items().stream().map(DBMapper::toDb).toList(), order.totalPrice(), order.customer(), order.placedAt(), toDb(order.shippingAddress()), toDb(order.billingAddress()), toDb(order.payment()));
    }

    public static Order toDomain(OrderDB order){
        return Order.reconstitute(order.id(), order.checkoutId(), order.customer(), order.placedAt(), toDomain(order.billingAddress()), toDomain(order.payment()), toDomain(order.shippingAddress()), order.items().stream().map(DBMapper::toDomain).toList());
    }

    public static AddressDTO toDto(AddressDB address){
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static ItemDTO toDto(ItemDB item){
        return new ItemDTO(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static OrderDTO toDto(OrderDB order){
        return new OrderDTO(order.id(), order.items().stream().map(DBMapper::toDto).toList(), order.totalPrice(), order.customer(), order.placedAt(), toDto(order.shippingAddress()), toDto(order.billingAddress()), toDto(order.payment()));
    }
}
