package com.github.eshopmodularmonolith.ordering.orders.contract;

import com.github.eshopmodularmonolith.ordering.orders.domain.Item;
import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

public class DTOMapper {


    private DTOMapper() {

    }

    public static AddressDTO toDto(Address address){
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static Address toDomain(AddressDTO address){
        return new Address(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static PaymentDTO toDto(Payment payment) {
        return new PaymentDTO(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }

    public static Payment toDomain(PaymentDTO payment) {
        return new Payment(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }

    public static ItemDTO toDto(Item item){
        return new ItemDTO(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static Item toDomain(ItemDTO item){
        return new Item(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static OrderDTO toDto(Order order){
        return new OrderDTO(order.id(), order.items().stream().map(DTOMapper::toDto).toList(), order.customerId(), order.name(), toDto(order.shippingAddress()), toDto(order.billingAddress()), toDto(order.payment()));
    }
}
