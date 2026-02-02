package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeProductWriteRepository implements ProductWriteRepository {

    private Map<UUID, Product> products;

    public FakeProductWriteRepository() {
        this.products = new HashMap<>();
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return Optional.ofNullable(products.get(id));
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