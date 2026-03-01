package com.github.eshopmodularmonolith.ordering.orders.application.query;

import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface OrderQueryRepository {

    PageResult<OrderDTO> paginatedOrders(PageRequest pageRequest);
    Optional<OrderDTO> findById(UUID id);
}
