package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductDTO;

public class ProductDtoToInfoMapper {

    private ProductDtoToInfoMapper(){

    }

    public static ProductInfo toInfo(ProductDTO product){
        return new ProductInfo(product.id(), product.name(), product.price());
    }
}
