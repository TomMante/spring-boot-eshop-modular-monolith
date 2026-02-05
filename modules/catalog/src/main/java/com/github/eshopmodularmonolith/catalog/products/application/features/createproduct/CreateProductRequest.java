package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        String name,
        List<String> categories,
        String description,
        String imageFile,
        BigDecimal price) {
}
