package io.github.tommante.eshopmodularmonolith.basket.basket.domain.events;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BasketCheckedOut(
        UUID eventId,
        Instant occurredOn,
        String userName,
        List<Item> items,
        BigDecimal totalPrice,
        Address billingAddress,
        Payment payment,
        Address shippingAddress) implements DomainEvent {

    public BasketCheckedOut(String userName, List<Item> items, BigDecimal totalPrice, Address billingAddress, Payment payment, Address shippingAddress) {
        this(UUID.randomUUID(), Instant.now(), userName, items, totalPrice, billingAddress, payment, shippingAddress);
    }

    @Override
    public String eventType() {
        return "BasketCheckedOut";
    }
}
