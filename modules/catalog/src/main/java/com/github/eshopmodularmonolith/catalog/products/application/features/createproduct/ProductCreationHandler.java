package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.model.Product;
import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import org.springframework.stereotype.Component;

//TODO Can I make this Private or package private?
@Component
public class ProductCreationHandler {


    private ProductWriteRepository repository;

    public ProductCreationHandler(ProductWriteRepository repository) {
        this.repository = repository;
    }

    public boolean handle(CreateProductCommand cmd) {
        var product = Product.create(cmd.id(), cmd.name(), cmd.categories(), cmd.description(), cmd.imageFile(), cmd.price());
        repository.save(product);
        return true;
    }
}
