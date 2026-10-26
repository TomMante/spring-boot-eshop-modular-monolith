package io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeRequest(UUID checkoutId, String paymentMethodId, BigDecimal amount) {
}
