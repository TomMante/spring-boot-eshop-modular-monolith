package com.github.eshopmodularmonolith.basket.basket.contract.integrationevents;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketCheckoutIntegrationEvent(UUID eventId, String userName, List<ItemDTO> items, BigDecimal totalPrice) {
}
