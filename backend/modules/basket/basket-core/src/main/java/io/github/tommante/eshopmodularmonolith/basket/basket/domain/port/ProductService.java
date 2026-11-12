package io.github.tommante.eshopmodularmonolith.basket.basket.domain.port;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;

import java.util.Optional;
import java.util.UUID;

//This servers as an Anti Corruption Layer, to get information about the products
public interface ProductService {

    Optional<ProductInfo> fetchProductInfo(UUID productId);
}
