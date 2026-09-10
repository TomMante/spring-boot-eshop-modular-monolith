package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import com.github.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductMapper.toReadModel;
import static com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductMapper.updateReadModel;

@Repository
public class JpaProductRepository implements ProductRepository {


    private final SpringJpaProductRepository repository;
    private final DomainEventPublisher localPublisher;

    public JpaProductRepository(SpringJpaProductRepository repository, DomainEventPublisher localPublisher) {
        this.repository = repository;
        this.localPublisher = localPublisher;
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return repository.findById(id).map(ProductMapper::toDomain);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    @Override
    public void save(Product product) {
        // An existing row is loaded and mutated rather than overwritten with a projection built from the domain:
        // ProductDB does not carry every column, so merging one would write nulls over the columns it omits.
        repository.findById(product.id()).ifPresentOrElse(
                storedProduct -> updateReadModel(storedProduct, product),
                () -> repository.save(toReadModel(product)));
        var events = product.clearDomainEvents();


        // local in process dispatch
        localPublisher.publish(events);
    }

    @Override
    public void delete(UUID productId) {
        repository.deleteById(productId);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }
}
