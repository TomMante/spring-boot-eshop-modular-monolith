package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.DBMapper;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoBasketRepository implements BasketRepository {

    private MongoTemplate mongoTemplate;

    public MongoBasketRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(ShoppingCart cart) {
        mongoTemplate.save(DBMapper.toDb(cart));
    }

    @Override
    public Optional<ShoppingCart> findById(UUID id) {
        return Optional.ofNullable(mongoTemplate.findById(id, ShoppingCartDB.class)).map(DBMapper::toDomain);
    }
}
