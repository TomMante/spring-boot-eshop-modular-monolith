package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeBasketRepository implements BasketRepository {

    private Map<String, ShoppingCart> carts;
    private List<DomainEvent> domainEvents;

    public List<DomainEvent> domainEvents() {
        return domainEvents;
    }

    public FakeBasketRepository() {
        carts = new HashMap<>();
        domainEvents = new ArrayList<>();
    }

    @Override
    public void save(ShoppingCart cart) {
        domainEvents.addAll(cart.clearDomainEvents());
        carts.put(cart.id(), cart);
    }

    @Override
    public Optional<ShoppingCart> findByUsername(String userName) {
        return Optional.ofNullable(carts.get(userName));
    }

    @Override
    public void delete(String userName) {
        carts.remove(userName);
    }

    @Override
    public void delete(ShoppingCart cart) {
        domainEvents.addAll(cart.clearDomainEvents());
        carts.remove(cart.id());
    }
}
