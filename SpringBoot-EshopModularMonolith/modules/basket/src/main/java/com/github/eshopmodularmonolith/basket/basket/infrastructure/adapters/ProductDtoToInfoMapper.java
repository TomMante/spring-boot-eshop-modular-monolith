package com.github.eshopmodularmonolith.basket.basket.infrastructure.adapters;

import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;

public class ProductDtoToInfoMapper {

    private ProductDtoToInfoMapper(){

    }

    public static ProductInfo toInfo(ProductDTO product){
        return new ProductInfo(product.id(), product.name(), product.price());
    }
}
