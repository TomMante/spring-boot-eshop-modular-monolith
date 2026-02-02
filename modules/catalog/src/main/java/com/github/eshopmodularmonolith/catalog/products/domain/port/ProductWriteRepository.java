package com.github.eshopmodularmonolith.catalog.products.domain.port;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductWriteRepository {

    Optional<Product> findProductById(UUID id);
    void save(Product product);
    void delete(UUID productId);
}
