package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakeProductWriteRepository implements ProductWriteRepository {

    private Map<UUID, Product> products;

    public FakeProductWriteRepository() {
        this.products = new HashMap<>();
    }

    @Override
    public Product getProductById(UUID id) {
        return products.get(id);
    }

    @Override
    public boolean save(Product product) {
        products.put(product.id(), product);
        return true;
    }
}