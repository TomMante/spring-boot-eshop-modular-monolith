package io.github.tommante.eshopmodularmonolith.catalog.products.domain.model;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.shared.domain.AggregateRoot;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Product extends AggregateRoot<UUID> {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String name;
    private final List<String> categories;
    private final String description;
    private final String imageFile;
    private final BigDecimal price;


    private Product(UUID id, String name, List<String> categories, String description, String imageFile, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.categories = categories;
        this.description = description;
        this.imageFile = imageFile;
        this.price = price;
    }

    @Override
    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<String> categories() {
        return categories;
    }

    public String description() {
        return description;
    }

    public String imageFile() {
        return imageFile;
    }

    public BigDecimal price() {
        return price;
    }


    //FIXME id should probably be created within the create method and not passed
    public static Product create(UUID id, String name, List<String> categories, String description, String imageFile, BigDecimal price, Instant createdAt) {
        var product = new Product(id, name, categories, description, imageFile, price);
        product.addDomainEvent(new ProductCreated(product.id, createdAt));
        return product;
    }

    public static Product reconstitute(UUID id, String name, List<String> categories, String description, String imageFile, BigDecimal price) {
        return new Product(id, name, categories, description, imageFile, price);
    }

    public Product update(String name, List<String> categories, String description, String imageFile, BigDecimal price){
        return new Product(this.id, name, categories, description, imageFile, price);
    }

}
