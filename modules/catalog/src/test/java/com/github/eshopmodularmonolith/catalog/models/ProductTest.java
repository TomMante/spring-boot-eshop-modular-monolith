package com.github.eshopmodularmonolith.catalog.models;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ProductTest {

    @Test
    @DisplayName("Given id, when product is created, then id is set")
    void givenId_whenProductIsCreated_thenIdIsSet(){
        var id = UUID.randomUUID();
        var product = new Product(id);

        Assertions.assertThat(product).extracting(Product::id).isEqualTo(id);
    }

}