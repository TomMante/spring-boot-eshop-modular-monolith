package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = "{catalog.product.name.notBlank}")
        String name,
        List<String> categories,
        String description,
        String imageFile,
        @NotNull(message = "{catalog.product.price.notNull}")
        BigDecimal price) {
}
