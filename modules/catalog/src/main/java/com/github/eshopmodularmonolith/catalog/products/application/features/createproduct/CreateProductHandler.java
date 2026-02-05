package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateProductHandler {


    private ProductWriteRepository repository;

    public CreateProductHandler(ProductWriteRepository repository) {
        this.repository = repository;
    }

    public UUID handle(CreateProductCommand cmd) {
        if(repository.existsByName(cmd.name())) throw new ProductAlreadyExistsException("Product with name {" + cmd.name() + "} already exists.");
        var product = Product.create(UUID.randomUUID(), cmd.name(), cmd.categories(), cmd.description(), cmd.imageFile(), cmd.price());
        repository.save(product);
        return product.id();
    }
}
