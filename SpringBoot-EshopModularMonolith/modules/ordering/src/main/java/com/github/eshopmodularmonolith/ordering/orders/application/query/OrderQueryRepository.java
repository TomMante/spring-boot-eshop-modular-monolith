package com.github.eshopmodularmonolith.ordering.orders.application.query;

import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;

import java.util.Optional;
import java.util.UUID;

public interface OrderQueryRepository {

    Optional<OrderDTO> findById(UUID id);
}
