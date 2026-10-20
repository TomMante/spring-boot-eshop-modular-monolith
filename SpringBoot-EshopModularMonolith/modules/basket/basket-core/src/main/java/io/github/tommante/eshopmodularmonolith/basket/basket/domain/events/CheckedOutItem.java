package io.github.tommante.eshopmodularmonolith.basket.basket.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckedOutItem(UUID id, UUID productId, int quantity, BigDecimal price, String productName) {
}
