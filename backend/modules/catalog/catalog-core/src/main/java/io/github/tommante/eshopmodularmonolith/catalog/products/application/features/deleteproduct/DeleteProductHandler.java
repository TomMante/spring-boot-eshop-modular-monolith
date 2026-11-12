package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
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
