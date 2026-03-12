package com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductQueryService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeProductQueryService implements ProductQueryService {

    private Map<UUID, ProductDTO> products;

    public FakeProductQueryService() {
        this.products = new HashMap<>();
    }

    public void addProduct(ProductDTO product){
        products.put(product.id(), product);
    }

    @Override
    public Optional<ProductDTO> findProduct(UUID id) {
        return Optional.ofNullable(products.get(id));
    }
}
