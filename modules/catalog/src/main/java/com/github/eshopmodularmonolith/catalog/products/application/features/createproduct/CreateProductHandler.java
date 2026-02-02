package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import org.springframework.stereotype.Component;

@Component
public class CreateProductHandler {


    private ProductWriteRepository repository;

    public CreateProductHandler(ProductWriteRepository repository) {
        this.repository = repository;
    }

    public void handle(CreateProductCommand cmd) {
        var product = Product.create(cmd.id(), cmd.name(), cmd.categories(), cmd.description(), cmd.imageFile(), cmd.price());
        repository.save(product);
    }
}
