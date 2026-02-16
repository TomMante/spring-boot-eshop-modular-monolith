package com.github.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class DeleteProductHandler
{
    private ProductRepository repository;

    public DeleteProductHandler(ProductRepository repository) {
        this.repository = repository;
    }

    public void handle(DeleteProductCommand cmd) {
        repository.delete(cmd.id());
    }
}
