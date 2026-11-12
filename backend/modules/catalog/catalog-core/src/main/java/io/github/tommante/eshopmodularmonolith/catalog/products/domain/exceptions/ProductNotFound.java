package io.github.tommante.eshopmodularmonolith.catalog.products.domain.exceptions;

import java.util.UUID;

public class ProductNotFound extends RuntimeException {

    public ProductNotFound(UUID productId) {
        super("Product with id [" + productId + "] not found.");
    }
}
