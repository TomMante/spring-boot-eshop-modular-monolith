package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringMongoOrderRepository extends MongoRepository<OrderDB, UUID> {
}
