package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorders;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;

public record GetOrdersQuery(CurrentUser viewer, int pageIndex, int pageSize) {
}
