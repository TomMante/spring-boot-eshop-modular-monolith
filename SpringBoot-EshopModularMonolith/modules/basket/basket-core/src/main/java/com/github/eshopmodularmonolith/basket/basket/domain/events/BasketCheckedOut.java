package com.github.eshopmodularmonolith.basket.basket.domain.events;

import com.github.eshopmodularmonolith.basket.basket.domain.model.Item;
import com.github.eshopmodularmonolith.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BasketCheckedOut(
        UUID eventId,
        Instant occurredOn,
        String userName,
        List<Item> items,
        BigDecimal totalPrice) implements DomainEvent {

    public BasketCheckedOut(String userName, List<Item> items, BigDecimal totalPrice) {
        this(UUID.randomUUID(), Instant.now(), userName, items, totalPrice);
    }

    @Override
    public String eventType() {
        return "BasketCheckedOut";
    }
}
