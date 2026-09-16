package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.ProductMother;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.Product;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class UpdateProductHandlerTest {

    private ProductRepository repository;
    private UpdateProductHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository();
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

    @Test
    @DisplayName("Given no product with the command id, when handler is called, then product is not found and nothing is saved")
    void givenNoProductWithTheCommandId_whenHandlerIsCalled_thenProductIsNotFoundAndNothingIsSaved() {
        var unknownProductId = UUID.randomUUID();
        var cmd = new UpdateProductCommand(
                unknownProductId,
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        assertThatExceptionOfType(ProductNotFound.class)
                .isThrownBy(() -> handler.handle(cmd))
                .withMessageContaining(unknownProductId.toString());

        assertThat(repository.findProductById(unknownProductId)).isEmpty();
    }

}
