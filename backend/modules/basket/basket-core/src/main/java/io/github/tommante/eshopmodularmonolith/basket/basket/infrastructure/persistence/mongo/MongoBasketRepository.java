package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.DBMapper;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MongoBasketRepository implements BasketRepository {

    private final SpringMongoBasketRepository repository;
    private final DomainEventPublisher domainEventPublisher;

    public MongoBasketRepository(SpringMongoBasketRepository repository, DomainEventPublisher domainEventPublisher) {
        this.repository = repository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public void save(ShoppingCart cart) {
        repository.save(DBMapper.toDb(cart));
    }

    @Override
    public Optional<ShoppingCart> findByUsername(String userName) {
        return repository.findById(userName).map(DBMapper::toDomain);
    }

    @Override
    public void delete(String userName) {
        repository.deleteById(userName);
    }

    @Override
    public void delete(ShoppingCart cart) {
        var domainEvents = cart.clearDomainEvents();
        domainEventPublisher.publish(domainEvents);
        delete(cart.userName());
    }
}
