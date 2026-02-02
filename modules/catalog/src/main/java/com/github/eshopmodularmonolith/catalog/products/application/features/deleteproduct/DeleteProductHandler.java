package com.github.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.port.ProductWriteRepository;
import org.springframework.stereotype.Component;

@Component
public class DeleteProductHandler
{
    private ProductWriteRepository repository;

    public DeleteProductHandler(ProductWriteRepository repository) {
        this.repository = repository;
    }

    public void handle(DeleteProductCommand cmd) {
        repository.delete(cmd.id());
    }
}
