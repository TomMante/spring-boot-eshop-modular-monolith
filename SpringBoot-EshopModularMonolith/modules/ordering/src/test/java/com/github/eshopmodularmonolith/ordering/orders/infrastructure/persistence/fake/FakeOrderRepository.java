package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderAlreadyExists;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeOrderRepository implements OrderRepository {

    private Map<UUID, Order> repository;

    public FakeOrderRepository() {
        repository = new HashMap<>();
    }

    public List<Order> orders() {
        return List.copyOf(repository.values());
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return Optional.ofNullable(repository.get(orderId));
    }

    @Override
    public void save(Order order) {
        if (isCheckoutAlreadyPlaced(order.checkoutId())) {
            throw new OrderAlreadyExists(order.checkoutId(), null);
        }
        repository.put(order.id(), order);
    }

    private boolean isCheckoutAlreadyPlaced(UUID checkoutId) {
        return repository.values().stream()
                .anyMatch(stored -> stored.checkoutId().equals(checkoutId));
    }

    @Override
    public void delete(UUID orderId) {
        repository.remove(orderId);
    }

    public void clear() {
        repository.clear();
    }
}
