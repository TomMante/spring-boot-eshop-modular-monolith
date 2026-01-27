package com.github.eshopmodularmonolith.catalog.products.features.createproduct;

import com.github.eshopmodularmonolith.catalog.products.application.features.createproduct.CreateProductCommand;
import com.github.eshopmodularmonolith.catalog.products.application.features.createproduct.ProductCreationHandler;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductWriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCreationTests {

    private ProductWriteRepository repository;
    private ProductCreationHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductWriteRepository();
        handler = new ProductCreationHandler(repository);
    }

    @Test
    @DisplayName("Given product information, when CreateProductCommand is sent, product is created.")
    void givenProductInformation_whenCreateCommandIsSent_thenProductIsCreated() {
        var providedId = UUID.randomUUID();
        var createProduct = new CreateProductCommand(
                providedId,
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var result = handler.handle(createProduct);
        assertThat(result).isTrue();

        var product = repository.getProductById(createProduct.id());

        assertThat(product.id()).isEqualTo(createProduct.id());
        assertThat(product.name()).isEqualTo(createProduct.name());
        assertThat(product.categories()).isEqualTo(createProduct.categories());
        assertThat(product.description()).isEqualTo(createProduct.description());
        assertThat(product.imageFile()).isEqualTo(createProduct.imageFile());
        assertThat(product.price()).isEqualTo(createProduct.price());
    }
}
