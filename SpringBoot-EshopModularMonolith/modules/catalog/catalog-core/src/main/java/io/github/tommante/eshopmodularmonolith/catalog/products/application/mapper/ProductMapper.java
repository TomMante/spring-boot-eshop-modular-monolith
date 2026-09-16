package io.github.tommante.eshopmodularmonolith.catalog.products.application.mapper;

import io.github.tommante.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.Product;

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
