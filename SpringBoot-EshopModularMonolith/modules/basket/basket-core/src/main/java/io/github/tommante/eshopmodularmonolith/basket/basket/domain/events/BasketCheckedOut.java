package io.github.tommante.eshopmodularmonolith.basket.basket.domain.events;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;
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
        Charge charge,
        Address shippingAddress) implements DomainEvent {

    @Override
    public String eventType() {
        return "BasketCheckedOut";
    }
}
