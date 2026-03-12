package com.github.eshopmodularmonolith.catalog.products.application.mapper;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;

public class ProductMapper {

    private ProductMapper(){}

    public static ProductDTO toProductDTO(Product product){
        return new ProductDTO(
                product.id(),
                product.name(),
                product.categories(),
                product.description(),
                product.imageFile(),
                product.price()
        );
    }
}
