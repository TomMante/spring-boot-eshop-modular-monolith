package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringMongoBasketRepository extends MongoRepository<ShoppingCartDB, String> {
}
