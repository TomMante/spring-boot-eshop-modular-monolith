package com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters;

import com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake.FakeProductQueryService;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogProductACLTest {

    private FakeProductQueryService productQueryService;
    private CatalogProductACL catalogProductACL;

    @BeforeEach
    void setUp() {
        productQueryService = new FakeProductQueryService();
        catalogProductACL = new CatalogProductACL(productQueryService);
    }

    @Test
    @DisplayName("Given existing product, when querying for the product, then get the product")
    void givenExistingProduct_whenQueryingForProduct_thenGetTheProduct(){
        var product = new ProductDTO(
                UUID.randomUUID(),
                "Hat",
                List.of("accessory"),
                "A nice hat.",
                "hat.png",
                BigDecimal.valueOf(783)
        );
        productQueryService.addProduct(product);
        assertThat(catalogProductACL.fetchProductInfo(product.id())).isPresent().get().usingRecursiveComparison().isEqualTo(ProductDtoToInfoMapper.toInfo(product));
    }

    @Test
    @DisplayName("Give product does not exist, when querying for the product, then empty is returned")
    void givenProductDoesNotExist_whenQueryingForProduct_thenEmptyIsReturned(){
        assertThat(catalogProductACL.fetchProductInfo(UUID.randomUUID())).isEmpty();
    }

}