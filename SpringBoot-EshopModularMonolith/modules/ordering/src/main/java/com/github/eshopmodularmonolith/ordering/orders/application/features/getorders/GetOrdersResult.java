package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;

import java.util.List;

public record GetOrdersResult(List<OrderDTO> orders, int pageIndex, int pageSize, long totalItems, int totalPages) {
}
