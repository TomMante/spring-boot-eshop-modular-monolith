package com.github.eshopmodularmonolith.catalog.products.application.query;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductQueryService;
import com.github.eshopmodularmonolith.catalog.products.domain.ProductDTOMother;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.fake.FakeProductQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCatalogTest {

    private FakeProductQueryRepository productQueryRepository;
    private ProductQueryService productQueryService;

    @BeforeEach
    void setUp() {
        productQueryRepository = new FakeProductQueryRepository();
        productQueryService = new ProductCatalog(productQueryRepository);
    }

    @Test
    @DisplayName("Given existing product, when querying for the product, then get the product")
    void givenExistingProduct_whenQueryingForProduct_thenGetTheProduct(){
        var product = ProductDTOMother.product();
        productQueryRepository.addProduct(product);
        assertThat(productQueryService.findProduct(product.id())).contains(product);
    }

    @Test
    @DisplayName("Give product does not exist, when querying for the product, then empty is returned")
    void givenProductDoesNotExist_whenQueryingForProduct_thenEmptyIsReturned(){
        assertThat(productQueryService.findProduct(UUID.randomUUID())).isEmpty();
    }

}