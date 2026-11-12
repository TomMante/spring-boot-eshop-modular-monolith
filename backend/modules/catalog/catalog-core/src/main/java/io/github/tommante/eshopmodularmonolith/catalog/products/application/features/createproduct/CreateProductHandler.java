package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.Product;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
public class CreateProductHandler {


    private ProductRepository repository;
    private final Clock clock;

    public CreateProductHandler(ProductRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public UUID handle(CreateProductCommand cmd) {
        if(repository.existsByName(cmd.name())) throw new ProductAlreadyExistsException("Product with name {" + cmd.name() + "} already exists.");
        var product = Product.create(UUID.randomUUID(), cmd.name(), cmd.categories(), cmd.description(), cmd.imageFile(), cmd.price(), clock.instant());
        repository.save(product);
        return product.id();
    }
}
