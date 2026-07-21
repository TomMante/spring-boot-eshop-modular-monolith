package com.github.eshopmodularmonolith.basket.basket.domain.port;

import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//This servers as an Anti Corruption Layer, to get information about the products
public interface ProductService {

    Optional<ProductInfo> fetchProductInfo(UUID productId);
}
