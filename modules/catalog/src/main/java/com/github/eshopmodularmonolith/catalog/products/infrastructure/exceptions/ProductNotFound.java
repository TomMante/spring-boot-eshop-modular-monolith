package com.github.eshopmodularmonolith.catalog.products.infrastructure.exceptions;

import java.util.UUID;

public class ProductNotFound extends RuntimeException {

    public ProductNotFound(UUID productId) {
        super("Product with id [" + productId + "] not found.");
    }
}
