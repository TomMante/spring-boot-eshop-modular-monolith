package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeOrderRepository implements OrderRepository {

    private Map<UUID, Order> repository;

    public FakeOrderRepository() {
        repository = new HashMap<>();
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return Optional.ofNullable(repository.get(orderId));
    }

    @Override
    public void save(Order order) {
        repository.put(order.id(), order);
    }
}
