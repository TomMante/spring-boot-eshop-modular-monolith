package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateProductCommand(UUID id, String name, List<String> categories, String description, String imageFile, BigDecimal price) {
}
