package io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ChargeDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketCheckoutIntegrationEvent(UUID eventId, String userName, List<CheckedOutItemDTO> items, BigDecimal totalPrice, AddressDTO billingAddress, ChargeDTO charge, AddressDTO shippingAddress) {
}
