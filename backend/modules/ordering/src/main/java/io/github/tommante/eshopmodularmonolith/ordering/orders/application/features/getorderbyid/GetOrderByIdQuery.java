package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;

import java.util.UUID;

public record GetOrderByIdQuery(CurrentUser viewer, UUID id) {
}
