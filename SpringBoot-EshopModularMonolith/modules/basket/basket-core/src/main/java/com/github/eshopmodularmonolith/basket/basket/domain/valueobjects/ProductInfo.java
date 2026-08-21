package com.github.eshopmodularmonolith.basket.basket.domain.valueobjects;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfo(UUID id, String name, BigDecimal price) {
}
