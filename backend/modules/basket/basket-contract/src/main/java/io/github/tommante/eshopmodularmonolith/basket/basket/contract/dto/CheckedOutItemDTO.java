package io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckedOutItemDTO(UUID id, UUID productId, int quantity, BigDecimal price, String productName) {
}
