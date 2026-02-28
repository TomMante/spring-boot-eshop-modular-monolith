package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeOrderQueryRepository implements OrderQueryRepository {

    private Map<UUID, OrderDTO> repository;

    public FakeOrderQueryRepository() {
        repository = new HashMap<>();
    }

    public void addOrder(OrderDTO order){
        repository.put(order.id(), order);
    }

    @Override
    public Optional<OrderDTO> findById(UUID id) {
        return Optional.ofNullable(repository.get(id));
    }
}
