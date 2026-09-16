package io.github.tommante.eshopmodularmonolith.basket.basket.contract.integrationevents;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ItemDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.PaymentDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BasketCheckoutIntegrationEvent(UUID eventId, String userName, List<ItemDTO> items, BigDecimal totalPrice, AddressDTO billingAddress, PaymentDTO payment, AddressDTO shippingAddress) {
}
