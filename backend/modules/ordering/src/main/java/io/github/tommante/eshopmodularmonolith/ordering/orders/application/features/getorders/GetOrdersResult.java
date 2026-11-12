package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorders;

import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.dto.OrderDTO;

import java.util.List;

public record GetOrdersResult(List<OrderDTO> orders, int pageIndex, int pageSize, long totalItems, int totalPages) {
}
