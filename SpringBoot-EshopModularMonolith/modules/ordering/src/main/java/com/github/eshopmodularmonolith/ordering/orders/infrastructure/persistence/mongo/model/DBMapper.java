package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model;

import com.github.eshopmodularmonolith.ordering.orders.contract.AddressDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.ItemDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import com.github.eshopmodularmonolith.ordering.orders.contract.PaymentDTO;
import com.github.eshopmodularmonolith.ordering.orders.domain.Item;
import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

public class DBMapper {


    public static AddressDB toDb(Address address){
        return new AddressDB(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static Address toDomain(AddressDB address){
        return new Address(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static PaymentDB toDb(Payment payment) {
        return new PaymentDB(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }

    public static Payment toDomain(PaymentDB payment) {
        return new Payment(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }

    public static ItemDB toDb(Item item){
        return new ItemDB(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static Item toDomain(ItemDB item){
        return new Item(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static OrderDB toDb(Order order){
        return new OrderDB(order.id(), order.items().stream().map(DBMapper::toDb).toList(), order.customer(), order.placedAt(), toDb(order.shippingAddress()), toDb(order.billingAddress()), toDb(order.payment()));
    }

    public static Order toDomain(OrderDB order){
        return Order.reconstitute(order.id(), order.customer(), order.placedAt(), toDomain(order.billingAddress()), toDomain(order.payment()), toDomain(order.shippingAddress()), order.items().stream().map(DBMapper::toDomain).toList());
    }

    public static AddressDTO toDto(AddressDB address){
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static PaymentDTO toDto(PaymentDB payment) {
        return new PaymentDTO(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }

    public static ItemDTO toDto(ItemDB item){
        return new ItemDTO(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static OrderDTO toDto(OrderDB order){
        return new OrderDTO(order.id(), order.items().stream().map(DBMapper::toDto).toList(), order.customer(), order.placedAt(), toDto(order.shippingAddress()), toDto(order.billingAddress()), toDto(order.payment()));
    }
}
