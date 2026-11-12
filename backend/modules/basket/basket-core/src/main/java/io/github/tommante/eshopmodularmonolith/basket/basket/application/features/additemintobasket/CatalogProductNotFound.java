package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.additemintobasket;

import java.util.UUID;

public class CatalogProductNotFound extends RuntimeException {
    public CatalogProductNotFound(UUID productId) {
        super(
                "Product: {id: " + productId.toString() + " } not found."
        );
    }
}
