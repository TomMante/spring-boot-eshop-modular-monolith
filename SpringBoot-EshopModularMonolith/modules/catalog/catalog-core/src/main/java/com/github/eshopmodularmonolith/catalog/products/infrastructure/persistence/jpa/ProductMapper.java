package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductDTO;
import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;

public final class ProductMapper {

    private ProductMapper() {

    }

    public static Product toDomain(ProductDB productDB) {
        return Product.reconstitute(productDB.id(), productDB.name(), productDB.categories(), productDB.description(), productDB.imageFile(), productDB.price());
    }

    public static ProductDB toReadModel(Product product){
        return new ProductDB(product.id(), product.name(), product.categories(), product.description(), product.imageFile(), product.price());
    }

    public static void updateReadModel(ProductDB storedProduct, Product product) {
        storedProduct.update(product.name(), product.categories(), product.description(), product.imageFile(),
                product.price());
    }

    public static ProductDTO toDto(ProductDB productDB){
        return new ProductDTO(productDB.id(), productDB.name(), productDB.categories(), productDB.description(), productDB.imageFile(), productDB.price());
    }
}
