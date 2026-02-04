package com.github.eshopmodularmonolith.catalog.products.application.features.getproducts;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetProductsHandlerTest {

    private FakeProductQueryRepository repository;
    private GetProductsHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeProductQueryRepository();
        handler = new GetProductsHandler(repository);
    }

    @Test
    @DisplayName("Given get products query, when handler is called, then get products result is return")
    void givenGetProductsQuery_whenHandlerIsCalled_thenGetProductsResultIsReturned() {
        var product1 = ProductMother.product();
        repository.addProduct(product1);
        var product2 = ProductMother.product();
        repository.addProduct(product2);
        var product3 = ProductMother.product();
        repository.addProduct(product3);

        var page1 = handler.handle(new GetProductsQuery(0, 2));
        assertThat(page1.products()).containsAnyElementsOf(List.of(product1, product2));
        assertThat(page1.pageIndex()).isZero();
        assertThat(page1.pageSize()).isEqualTo(2);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);

        var page2 = handler.handle(new GetProductsQuery(1, 2));
        assertThat(page2.products()).containsAnyElementsOf(List.of(product3));
        assertThat(page2.pageIndex()).isOne();
        assertThat(page2.pageSize()).isEqualTo(2);
        assertThat(page2.totalItems()).isEqualTo(3);
        assertThat(page2.totalPages()).isEqualTo(2);

    }
}