package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.ProductMother;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.Product;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteProductHandlerTest {

    private ProductRepository repository;
    private DeleteProductHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository();
        handler = new DeleteProductHandler(repository);
    }

    @Test
    @DisplayName("Given delete product command, when handler is called, then product is created")
    void givenDeleteProductCommand_whenHandlerIsCalled_thenProductIsCreated_() {
        var existingProduct = ProductMother.product();
        repository.save(existingProduct);
        assertThat(repository.findProductById(existingProduct.id())).isPresent().get().extracting(Product::id).isEqualTo(existingProduct.id());
        var cmd = new DeleteProductCommand(existingProduct.id());
        handler.handle(cmd);
        assertThat(repository.findProductById(existingProduct.id())).isEmpty();
    }
}