package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(UUID id, String name, List<String> categories, String description, String imageFile,
                                   BigDecimal price) {
}
