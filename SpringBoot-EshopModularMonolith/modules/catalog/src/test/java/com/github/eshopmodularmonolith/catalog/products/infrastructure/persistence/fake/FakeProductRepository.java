package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeProductRepository implements ProductRepository {

    private Map<UUID, Product> products;

    public FakeProductRepository() {
        this.products = new HashMap<>();
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public boolean existsByName(String name) {
        return products.values().stream().anyMatch(p -> p.name().equals(name));
    }

    @Override
    public void save(Product product) {
        products.put(product.id(), product);
    }

    @Override
    public void delete(UUID productId) {
        products.remove(productId);
    }
}