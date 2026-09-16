package com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder;

import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;

import java.util.UUID;

public record DeleteOrderCommand(CurrentUser requester, UUID id) {
}
