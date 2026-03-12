package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCart;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.DBMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoBasketRepository implements BasketRepository {

    private final SpringMongoBasketRepository repository;

    public MongoBasketRepository(SpringMongoBasketRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(ShoppingCart cart) {
        repository.save(DBMapper.toDb(cart));
    }

    @Override
    public Optional<ShoppingCart> findByUserName(String userName) {
        return repository.findById(userName).map(DBMapper::toDomain);
    }

    @Override
    public void delete(String userName) {
        repository.deleteById(userName);
    }
}
