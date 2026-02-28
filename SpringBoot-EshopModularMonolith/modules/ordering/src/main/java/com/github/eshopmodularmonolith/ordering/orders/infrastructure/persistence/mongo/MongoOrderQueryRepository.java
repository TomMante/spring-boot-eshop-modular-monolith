package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoOrderQueryRepository implements OrderQueryRepository {

    private MongoTemplate template;

    public MongoOrderQueryRepository(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public Optional<OrderDTO> findById(UUID id) {
        return Optional.ofNullable(template.findById(id, OrderDB.class)).map(DBMapper::toDto);
    }
}
