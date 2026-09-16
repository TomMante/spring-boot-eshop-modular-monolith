package com.github.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;

import java.util.UUID;

public record GetOrderByIdQuery(CurrentUser viewer, UUID id) {
}
