package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductQueryService;
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
