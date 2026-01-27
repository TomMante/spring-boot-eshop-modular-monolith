package com.github.eshopmodularmonolith.catalog.products.domain.model;

import com.github.eshopmodularmonolith.shared.ddd.AggregateRoot;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product implements AggregateRoot<UUID> {

    @EqualsAndHashCode.Include
    private UUID id;
    private String name;
    private List<String> categories;
    private String description;
    private String imageFile;
    private BigDecimal price;


    private Product(UUID id, String name, List<String> categories, String description, String imageFile, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.categories = categories;
        this.description = description;
        this.imageFile = imageFile;
        this.price = price;
    }

    public static Product create(UUID id, String name, List<String> categories, String description, String imageFile, BigDecimal price) {
        return new Product(id, name, categories, description, imageFile, price);
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
}
