package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.exceptions.ProductNotFound;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductMapper.toDomain;
import static com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductMapper.toReadModel;

@Repository
public class JpaProductWriteRepository implements ProductWriteRepository {


    private final SpringJpaProductWriteRepository repository;

    public JpaProductWriteRepository(SpringJpaProductWriteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product getProductById(UUID id) {
        return toDomain(repository.findById(id).orElseThrow(() -> new ProductNotFound(id)));
    }

    @Override
    public boolean save(Product product) {
        repository.save(toReadModel(product));
        return true;
    }
}
