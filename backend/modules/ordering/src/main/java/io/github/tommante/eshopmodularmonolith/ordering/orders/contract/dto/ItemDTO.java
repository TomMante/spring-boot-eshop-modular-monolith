package io.github.tommante.eshopmodularmonolith.ordering.orders.contract.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDTO(UUID id, UUID productId, int quantity, BigDecimal price) {
}