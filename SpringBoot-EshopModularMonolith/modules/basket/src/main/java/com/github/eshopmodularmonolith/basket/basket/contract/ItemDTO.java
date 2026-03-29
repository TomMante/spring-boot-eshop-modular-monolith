package com.github.eshopmodularmonolith.basket.basket.contract;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDTO(UUID id, UUID productId, int quantity, String color, BigDecimal priceWhenAdded, String productName) {
}
