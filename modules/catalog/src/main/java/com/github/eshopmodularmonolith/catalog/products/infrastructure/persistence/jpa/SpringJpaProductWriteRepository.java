package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface SpringJpaProductWriteRepository extends JpaRepository<ProductDB, UUID> {

    @Override
    Optional<ProductDB> findById(UUID uuid);
}
