package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import org.springframework.stereotype.Component;

@Component
public class UpdateProductHandler {

    private ProductRepository repository;

    public UpdateProductHandler(ProductRepository repository) {
        this.repository = repository;
    }

    public void handle(UpdateProductCommand command){
        repository.findProductById(command.id()).map(p ->
                p.update(command.name(), command.categories(), command.description(), command.imageFile(), command.price()))
                .ifPresentOrElse(repository::save, () -> {
                    throw new ProductNotFound(command.id());
                });
    }
}
