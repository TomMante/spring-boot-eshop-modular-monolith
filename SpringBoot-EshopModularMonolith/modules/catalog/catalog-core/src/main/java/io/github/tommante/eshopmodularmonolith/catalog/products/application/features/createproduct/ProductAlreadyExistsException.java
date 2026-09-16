package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}
