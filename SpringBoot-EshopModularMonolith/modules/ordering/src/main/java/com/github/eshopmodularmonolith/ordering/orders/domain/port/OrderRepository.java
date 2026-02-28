package com.github.eshopmodularmonolith.ordering.orders.domain.port;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Optional<Order> findById(UUID orderId);
    void save(Order order);
    void delete(UUID orderId);
}
