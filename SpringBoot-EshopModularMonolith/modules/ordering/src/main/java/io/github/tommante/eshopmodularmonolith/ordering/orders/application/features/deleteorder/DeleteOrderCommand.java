package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;

import java.util.UUID;

public record DeleteOrderCommand(CurrentUser requester, UUID id) {
}
