package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

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
        return new OrderDB(order.id(), order.items().stream().map(DBMapper::toDb).toList(), order.customerId(), order.name(), toDb(order.shippingAddress()), toDb(order.billingAddress()), toDb(order.payment()));
    }

    public static Order toDomain(OrderDB order){
        return Order.reconstitute(order.id(), order.customerId(), order.name(), toDomain(order.shippingAddress()), toDomain(order.billingAddress()), toDomain(order.payment()), order.items().stream().map(DBMapper::toDomain).toList());
    }
}
