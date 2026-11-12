package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductCommand(String name, List<String> categories, String description, String imageFile, BigDecimal price) {
}
