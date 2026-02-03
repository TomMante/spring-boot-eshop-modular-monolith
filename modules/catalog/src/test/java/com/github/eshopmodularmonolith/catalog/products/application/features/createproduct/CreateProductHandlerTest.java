package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

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

class CreateProductHandlerTest {


    private ProductWriteRepository repository;
    private CreateProductHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductWriteRepository();
        handler = new CreateProductHandler(repository);
    }

    @Test
    @DisplayName("Given create product command, when handler is called, then product is created")
    void givenCreateProductCommand_whenHandlerIsCalled_thenProductIsCreated() {
        var cmd = new CreateProductCommand(
                UUID.randomUUID(),
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        assertThat(repository.findProductById(cmd.id())).isEmpty();
        handler.handle(cmd);
        assertThat(repository.findProductById(cmd.id())).isPresent().get().extracting(Product::id).isEqualTo(cmd.id());
    }
}