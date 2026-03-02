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

    private SpringMongoBasketRepository repository;

    public MongoBasketRepository(SpringMongoBasketRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(ShoppingCart cart) {
        repository.save(DBMapper.toDb(cart));
    }

    @Override
    public Optional<ShoppingCart> findById(UUID id) {
        return repository.findById(id).map(DBMapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
