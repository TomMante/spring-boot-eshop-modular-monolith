package io.github.tommante.eshopmodularmonolith.ordering.orders.contract;

import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Item;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.Order;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.valueobjects.Payment;

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
        var card = payment.card();
        return new PaymentDTO(payment.chargeReference(), payment.amount(),
                new CardSummaryDTO(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }

    public static Payment toDomain(PaymentDTO payment) {
        var card = payment.card();
        return new Payment(payment.chargeReference(), payment.amount(),
                new CardSummary(card.brand(), card.last4(), card.expiryMonth(), card.expiryYear()));
    }

    public static ItemDTO toDto(Item item){
        return new ItemDTO(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static Item toDomain(ItemDTO item){
        return new Item(item.id(), item.productId(), item.quantity(), item.price());
    }

    public static OrderDTO toDto(Order order){
        return new OrderDTO(order.id(), order.items().stream().map(DTOMapper::toDto).toList(), order.totalPrice(), order.customer(), order.placedAt(), toDto(order.shippingAddress()), toDto(order.billingAddress()), toDto(order.payment()));
    }
}
