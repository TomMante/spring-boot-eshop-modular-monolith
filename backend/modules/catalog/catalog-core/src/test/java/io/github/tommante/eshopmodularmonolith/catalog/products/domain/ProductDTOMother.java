package io.github.tommante.eshopmodularmonolith.catalog.products.domain;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.dto.ProductDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductDTOMother {

    private ProductDTOMother() {}


    public static ProductDTO product() {
        return builder().build();
    }
    public static ProductDTO productWithName(String name) {
        return builder().withName(name).build();
    }

    public static ProductDTOBuilder builder() {
        return new ProductDTOBuilder();
    }

    public static class ProductDTOBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Galaxy";
        private List<String> categories = new ArrayList<>(List.of("cat1", "cat2"));
        private String description = "Galaxy description";
        private String imageFile = "galaxy.png";
        private BigDecimal price = BigDecimal.valueOf(550);


        public ProductDTOBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ProductDTO build() {
            return new ProductDTO(id, name, categories, description, imageFile, price);
        }
    }
}
