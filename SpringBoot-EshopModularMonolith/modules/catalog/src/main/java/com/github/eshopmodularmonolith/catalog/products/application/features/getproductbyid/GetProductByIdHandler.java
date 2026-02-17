package com.github.eshopmodularmonolith.catalog.products.application.features.getproductbyid;

import com.github.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import com.github.eshopmodularmonolith.catalog.products.application.query.ProductQueryRepository;
import org.springframework.stereotype.Component;

@Component
public class GetProductByIdHandler {

    private final ProductQueryRepository repository;


    public GetProductByIdHandler(ProductQueryRepository repository) {
        this.repository = repository;
    }

    public GetProductByIdResult handle(GetProductByIdQuery query) {
        var result = repository.findById(query.productId());
        if(result.isEmpty()) throw  new ProductNotFound(query.productId());
        return new GetProductByIdResult(result.get());
    }
}
