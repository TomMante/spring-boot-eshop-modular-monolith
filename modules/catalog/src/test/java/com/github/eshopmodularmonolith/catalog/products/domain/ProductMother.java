package com.github.eshopmodularmonolith.catalog.products.domain;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductMother {

    private ProductMother() {}


    public static Product product() {
        return builder().build();
    }
    public static Product productWithName(String name) {
        return builder().withName(name).build();
    }

    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Galaxy";
        private List<String> categories = new ArrayList<>(List.of("cat1", "cat2"));
        private String description = "Galaxy description";
        private String imageFile = "galaxy.png";
        private BigDecimal price = BigDecimal.valueOf(550);


        public ProductBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public Product build() {
            return Product.create(id, name, categories, description, imageFile, price);
        }
    }
}
