package com.github.eshopmodularmonolith.catalog.products.domain.port;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;

import java.util.UUID;

public interface ProductWriteRepository {

    Product getProductById(UUID id);
    boolean save(Product product);
}
