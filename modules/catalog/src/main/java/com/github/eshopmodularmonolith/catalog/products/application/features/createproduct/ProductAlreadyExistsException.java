package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}
