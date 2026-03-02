package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface SpringMongoBasketRepository extends MongoRepository<ShoppingCartDB, UUID> {
}
