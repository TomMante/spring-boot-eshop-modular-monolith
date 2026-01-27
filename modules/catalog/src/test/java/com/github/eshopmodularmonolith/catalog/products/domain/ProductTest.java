package com.github.eshopmodularmonolith.catalog.products.domain;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

class ProductTest {

    @Test
    @DisplayName("Given id, when product is created, then id is set")
    void givenId_whenProductIsCreated_thenIdIsSet(){
        var id = UUID.randomUUID();
        var name = "Google Pixel";
        var categories = List.of("Phone");
        var description = "A google phone";
        var imageFile = "pixel.png";
        var price = BigDecimal.valueOf(500);
        var product = Product.create(id, name,categories ,description, imageFile, price );

        Assertions.assertThat(product).extracting(Product::id).isEqualTo(id);
        Assertions.assertThat(product).extracting(Product::categories).isEqualTo(categories);
        Assertions.assertThat(product).extracting(Product::description).isEqualTo(description);
        Assertions.assertThat(product).extracting(Product::imageFile).isEqualTo(imageFile);
        Assertions.assertThat(product).extracting(Product::price).isEqualTo(price);
    }

}