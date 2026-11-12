package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.query.BasketQueryRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.ShoppingCartDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.DBMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoBasketQueryRepository implements BasketQueryRepository {

    private SpringMongoBasketRepository repository;


    public MongoBasketQueryRepository(SpringMongoBasketRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ShoppingCartDTO> findByUsername(String userName) {
        return repository.findById(userName).map(DBMapper::toDTO);
    }
}
