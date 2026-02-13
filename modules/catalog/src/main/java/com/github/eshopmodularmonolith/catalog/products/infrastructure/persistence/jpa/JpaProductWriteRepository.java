package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import com.github.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductMapper.toReadModel;

@Repository
public class JpaProductWriteRepository implements ProductWriteRepository {


    private final SpringJpaProductRepository repository;
    private final DomainEventPublisher localPublisher;
    private final OutboxEventStore outbox;

    public JpaProductWriteRepository(SpringJpaProductRepository repository, DomainEventPublisher localPublisher,
                                     OutboxEventStore outbox) {
        this.repository = repository;
        this.localPublisher = localPublisher;
        this.outbox = outbox;
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return repository.findById(id).map(ProductMapper::toDomain);
    }

    @Transactional
    @Override
    public void save(Product product) {
        repository.save(toReadModel(product));
        var events = product.clearDomainEvents();


        // local in process dispatch
        localPublisher.publish(events);
        // persistent global dispatch
        events.forEach(outbox::save);
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
