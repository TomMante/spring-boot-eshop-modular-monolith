package com.github.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductWriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteProductHandlerTest {

    private ProductWriteRepository repository;
    private DeleteProductHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductWriteRepository();
        handler = new DeleteProductHandler(repository);
    }

    @Test
    @DisplayName("Given create product command, when handler is called, then product is created")
    void givenCreateProductCommand_whenHandlerIsCalled_thenProductIsCreated_() {
        var existingProduct = ProductMother.product();
        repository.save(existingProduct);
        assertThat(repository.findProductById(existingProduct.id())).isPresent().get().extracting(Product::id).isEqualTo(existingProduct.id());
        var cmd = new DeleteProductCommand(existingProduct.id());
        handler.handle(cmd);
        assertThat(repository.findProductById(existingProduct.id())).isEmpty();
    }
}