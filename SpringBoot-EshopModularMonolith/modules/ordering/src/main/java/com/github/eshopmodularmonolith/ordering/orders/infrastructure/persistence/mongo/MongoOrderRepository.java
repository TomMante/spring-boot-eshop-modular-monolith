package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoOrderRepository implements OrderRepository {

    private final SpringMongoOrderRepository springRepository;

    public MongoOrderRepository(SpringMongoOrderRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return springRepository.findById(orderId).map(DBMapper::toDomain);
    }

    @Override
    public void save(Order order) {
        springRepository.save(DBMapper.toDb(order));
    }
}
