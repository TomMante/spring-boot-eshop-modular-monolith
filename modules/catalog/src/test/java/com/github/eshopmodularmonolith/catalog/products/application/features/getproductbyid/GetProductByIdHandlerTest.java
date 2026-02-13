package com.github.eshopmodularmonolith.catalog.products.application.features.getproductbyid;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GetProductByIdHandlerTest {

    private FakeProductQueryRepository repository;
    private GetProductByIdHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductQueryRepository();
        handler = new GetProductByIdHandler(repository);
    }

    @Test
    @DisplayName("Given GetProductsByIdQuery, when handler is called, then GetProductsByIdResult is returned")
    void givenGetProductsByIdQuery_whenHandlerIsCalled_thenGetProductByIdResultIsReturned() {
        var product = ProductMother.product();
        repository.addProduct(product);
        var query = new GetProductByIdQuery(product.id());
        var result = handler.handle(query);

        var expected = Product.reconstitute(product.id(), product.name(), product.categories(), product.description(), product.imageFile(), product.price());
        assertThat(result).extracting(GetProductByIdResult::product).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("Given GetProductByIdQuery, when specified id is not present, then should throw an exception")
    void givenGetProductByIdQuery_whenSpecifiedIdIsNotPresent_thenShouldThrowAnException() {
        var query = new GetProductByIdQuery(UUID.randomUUID());
        assertThatExceptionOfType(ProductNotFound.class).isThrownBy(() -> handler.handle(query));
    }

}