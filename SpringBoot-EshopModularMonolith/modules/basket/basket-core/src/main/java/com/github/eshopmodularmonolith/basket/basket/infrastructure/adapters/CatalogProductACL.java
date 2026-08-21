package com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters;

import com.github.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CatalogProductACL implements ProductService {

    private ProductQueryService productQueryService;

    public CatalogProductACL(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @Override
    public Optional<ProductInfo> fetchProductInfo(UUID productId) {
        return productQueryService.findProduct(productId).map(ProductDtoToInfoMapper::toInfo);
    }
}
