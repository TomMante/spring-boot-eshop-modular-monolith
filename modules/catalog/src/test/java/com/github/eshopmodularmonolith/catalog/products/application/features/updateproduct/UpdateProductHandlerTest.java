package com.github.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import com.github.eshopmodularmonolith.catalog.products.application.features.createproduct.CreateProductCommand;
import com.github.eshopmodularmonolith.catalog.products.application.features.createproduct.CreateProductHandler;
import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductWriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateProductHandlerTest {

    private ProductWriteRepository repository;
    private UpdateProductHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductWriteRepository();
        handler = new UpdateProductHandler(repository);
    }

    @Test
    @DisplayName("Given create product command, when handler is called, then product is created")
    void givenCreateProductCommand_whenHandlerIsCalled_thenProductIsCreated() {
        var product = ProductMother.product();
        repository.save(product);

        var cmd = new UpdateProductCommand(
                product.id(),
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        assertThat(repository.findProductById(cmd.id())).isPresent();
        handler.handle(cmd);
        var updatedProduct = repository.findProductById(cmd.id()).orElseThrow();
        assertThat(updatedProduct).extracting(Product::name).isEqualTo(cmd.name());
        assertThat(updatedProduct).extracting(Product::categories).isEqualTo(cmd.categories());
        assertThat(updatedProduct).extracting(Product::description).isEqualTo(cmd.description());
        assertThat(updatedProduct).extracting(Product::imageFile).isEqualTo(cmd.imageFile());
        assertThat(updatedProduct).extracting(Product::price).isEqualTo(cmd.price());
    }

}