package io.github.tommante.eshopmodularmonolith.basket.basket.domain.events;

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
        List<CheckedOutItem> items,
        BigDecimal totalPrice,
        Address billingAddress,
        Payment payment,
        Address shippingAddress) implements DomainEvent {

    public BasketCheckedOut(Instant occurredOn, String userName, List<CheckedOutItem> items, BigDecimal totalPrice, Address billingAddress, Payment payment, Address shippingAddress) {
        this(UUID.randomUUID(), occurredOn, userName, items, totalPrice, billingAddress, payment, shippingAddress);
    }

    @Override
    public String eventType() {
        return "BasketCheckedOut";
    }
}
