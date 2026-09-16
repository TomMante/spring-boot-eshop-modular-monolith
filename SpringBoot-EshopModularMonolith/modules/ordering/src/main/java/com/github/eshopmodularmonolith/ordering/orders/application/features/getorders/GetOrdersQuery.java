package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;

public record GetOrdersQuery(CurrentUser viewer, int pageIndex, int pageSize) {
}
