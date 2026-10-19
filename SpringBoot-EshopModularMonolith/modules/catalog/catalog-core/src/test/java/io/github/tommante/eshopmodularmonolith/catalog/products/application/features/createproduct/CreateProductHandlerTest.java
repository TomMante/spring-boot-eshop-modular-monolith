package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CreateProductHandlerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");


    private ProductRepository repository;
    private CreateProductHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductRepository();
        handler = new CreateProductHandler(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Given product information, when CreateProductCommand is sent, product is created.")
    void givenProductInformation_whenCreateCommandIsSent_thenProductIsCreated() {
        var createProduct = new CreateProductCommand(
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var productId = handler.handle(createProduct);

        var productOpt = repository.findProductById(productId);
        assertThat(productOpt).isPresent();

        var product = productOpt.get();

        assertThat(product.id()).isEqualTo(productId);
        assertThat(product.name()).isEqualTo(createProduct.name());
        assertThat(product.categories()).isEqualTo(createProduct.categories());
        assertThat(product.description()).isEqualTo(createProduct.description());
        assertThat(product.imageFile()).isEqualTo(createProduct.imageFile());
        assertThat(product.price()).isEqualTo(createProduct.price());
        assertThat(product.domainEvents())
                .singleElement()
                .isInstanceOfSatisfying(ProductCreated.class, event -> assertThat(event.occurredOn()).isEqualTo(NOW));
    }

    @Test
    @DisplayName("Given product information, when CreateProductCommand is sent, product is created.")
    void whenProductWithSameNameExists_thenExceptionIsThrown() {
        var createProduct = new CreateProductCommand(
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var productId = handler.handle(createProduct);
        var productOpt = repository.findProductById(productId);
        assertThat(productOpt).isPresent();

        assertThatExceptionOfType(ProductAlreadyExistsException.class).isThrownBy(() -> handler.handle(createProduct));
    }
}