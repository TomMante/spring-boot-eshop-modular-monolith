package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;

public record GetOrdersResponse(PaginatedResults<OrderDTO> pagedOrders) {
}
