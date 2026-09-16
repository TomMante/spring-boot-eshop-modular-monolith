package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface SpringJpaProductRepository extends JpaRepository<ProductDB, UUID> {

    @Override
    Optional<ProductDB> findById(UUID uuid);

    Page<ProductDB> findAll(Pageable pageable);

    boolean existsByName(String name);
}
