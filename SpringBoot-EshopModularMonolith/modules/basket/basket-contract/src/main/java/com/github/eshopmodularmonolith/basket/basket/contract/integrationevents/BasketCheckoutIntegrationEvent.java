package com.github.eshopmodularmonolith.basket.basket.contract.integrationevents;

import com.github.eshopmodularmonolith.basket.basket.contract.ItemDTO;

import java.math.BigDecimal;
import java.util.List;

public record BasketCheckoutIntegrationEvent(String userName, List<ItemDTO> items, BigDecimal totalPrice) {
}
