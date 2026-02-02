package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductMapper.toReadModel;

@Repository
public class JpaProductWriteRepository implements ProductWriteRepository {


    private final SpringJpaProductWriteRepository repository;

    public JpaProductWriteRepository(SpringJpaProductWriteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return repository.findById(id).map(ProductMapper::toDomain);
    }

    @Override
    public void save(Product product) {
        repository.save(toReadModel(product));
    }

    @Override
    public void delete(UUID productId) {
        repository.deleteById(productId);
    }
}
