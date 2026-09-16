package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorders;

import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import io.github.tommante.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;

public record GetOrdersResponse(PaginatedResults<OrderDTO> pagedOrders) {
}
